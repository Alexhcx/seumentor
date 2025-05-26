package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.course.api.DisciplineQuery;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;

import com.projetointegrador.seumentor.tutoring.api.TutoringCommand;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.api.mapper.TutoringMapper;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.tutoring.exception.AvailabilityNotFoundException;
import com.projetointegrador.seumentor.tutoring.model.*;
import com.projetointegrador.seumentor.tutoring.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRatingRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;

import com.projetointegrador.seumentor.user.api.UserCommand;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.enums.Role;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TutoringCommandService implements TutoringCommand {

        private final TutoringRepository tutoringRepository;
        private final TutoringRatingRepository tutoringRatingRepository;
        private final TutoringParticipantsRepository tutoringParticipantsRepository;
        private final MentorAvailabilityRepository mentorAvailabilityRepository;

        private final UserQuery userQuery;
        private final DisciplineQuery disciplineQuery;
        private final TutoringQuery tutoringQuery;
        private final UserCommand userCommand;
        private final TutoringMapper tutoringMapper;

        private static final Map<StatusTutoring, Set<StatusTutoring>> MENTOR_ALLOWED_TRANSITIONS = new HashMap<>();

        static {
                MENTOR_ALLOWED_TRANSITIONS.put(StatusTutoring.A_MARCAR, Set.of(StatusTutoring.PENDENTE));
                MENTOR_ALLOWED_TRANSITIONS.put(StatusTutoring.PENDENTE,
                                Set.of(StatusTutoring.AGENDADA, StatusTutoring.CANCELADA));
                MENTOR_ALLOWED_TRANSITIONS.put(StatusTutoring.AGENDADA,
                                Set.of(StatusTutoring.EM_ANDAMENTO, StatusTutoring.CANCELADA));
                MENTOR_ALLOWED_TRANSITIONS.put(StatusTutoring.EM_ANDAMENTO,
                                Set.of(StatusTutoring.CONCLUIDA, StatusTutoring.CANCELADA));
                MENTOR_ALLOWED_TRANSITIONS.put(StatusTutoring.CONCLUIDA, Set.of());
                MENTOR_ALLOWED_TRANSITIONS.put(StatusTutoring.CANCELADA, Set.of());
        }

        private static final Logger log = LoggerFactory.getLogger(TutoringCommandService.class);

        @Override
        @Transactional
        public ScheduledTutoringRepresentation scheduleTutoring(ScheduleTutoringRequest request) throws Exception {
                log.info("Attempting to schedule tutoring with request: {}", request);

                if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
                        log.warn("Schedule failed: Start time must be before or equal to end time. Request: {}",
                                        request);
                        throw new TutoringOperationException(
                                        "Hora de início deve ser anterior e diferente da hora de fim.");
                }
                if (request.tutoringDate().isBefore(java.time.LocalDate.now())) {
                        log.warn("Schedule failed: Tutoring date is in the past. Request: {}", request);
                        throw new TutoringOperationException("A data da monitoria não pode ser no passado.");
                }
                if (request.mentorId().equals(request.menteeId())) {
                        log.warn("Schedule failed: Mentor and Mentee cannot be the same person. Request: {}", request);
                        throw new TutoringOperationException("O mentor e o mentorado não podem ser a mesma pessoa.");
                }

                User mentor;
                User mentee;
                Discipline discipline;
                try {
                        mentor = userQuery.getUserReferenceById(request.mentorId());
                        mentee = userQuery.getUserReferenceById(request.menteeId());
                        discipline = disciplineQuery.getReferenceById(request.disciplineId());
                } catch (UserNotFoundException | DisciplineNotFoundException e) {
                        log.warn("Schedule failed: Could not find User or Discipline. Request: {}, Error: {}", request,
                                        e.getMessage());
                        throw new TutoringOperationException(
                                        "Falha ao obter dados necessários (usuário ou disciplina): " + e.getMessage(),
                                        e);
                }

                List<StatusTutoring> statusesToCheckForConflict = Arrays.asList(
                                StatusTutoring.PENDENTE, StatusTutoring.AGENDADA, StatusTutoring.EM_ANDAMENTO);

                boolean alreadyExists = tutoringRepository
                                .existsByMentorAndDisciplineAndTutoringDateAndStartTimeAndEndTimeAndStatusNotIn(
                                                mentor,
                                                discipline,
                                                request.tutoringDate(),
                                                request.startTime(),
                                                request.endTime(),
                                                Arrays.asList(StatusTutoring.CANCELADA, StatusTutoring.CONCLUIDA));
                if (alreadyExists) {
                        log.warn("Schedule failed: Exact tutoring already exists and is active/pending. Request: {}",
                                        request);
                        throw new TutoringOperationException(
                                        "Esta mentoria (mesmo mentor, disciplina, data e horário) já existe e está pendente ou agendada.");
                }

                checkUserConflictsWithExistingTutorings(mentee, request.tutoringDate(), request.startTime(),
                                request.endTime(), statusesToCheckForConflict, "mentorado");
                checkUserConflictsWithExistingTutorings(mentor, request.tutoringDate(), request.startTime(),
                                request.endTime(), statusesToCheckForConflict, "mentor");

                DayWeek dayOfWeek = mapJavaDayOfWeekToCustom(request.tutoringDate().getDayOfWeek());
                Optional<MentorAvailability> coveringAvailability = mentorAvailabilityRepository
                                .findCoveringAndActiveAvailability(
                                                mentor,
                                                discipline,
                                                dayOfWeek,
                                                request.startTime(),
                                                request.endTime());

                if (coveringAvailability.isEmpty()) {
                        log.warn("Schedule failed: Mentor (ID: {}) does not have an active and covering availability for discipline '{}' on {} from {} to {}.",
                                        mentor.getId(), discipline.getDisciplineName(), dayOfWeek,
                                        request.startTime().format(TutoringMapper.TIME_FORMATTER),
                                        request.endTime().format(TutoringMapper.TIME_FORMATTER));
                        throw new TutoringOperationException(String.format(
                                        "O mentor não possui disponibilidade ativa para a disciplina '%s' que cubra o horário solicitado (%s das %s às %s).",
                                        discipline.getDisciplineName(),
                                        dayOfWeek.toString().toLowerCase().replace("_", " "),
                                        request.startTime().format(TutoringMapper.TIME_FORMATTER),
                                        request.endTime().format(TutoringMapper.TIME_FORMATTER)));
                }
                log.info("Found covering and active availability (ID: {}) for mentor {} for the requested tutoring.",
                                coveringAvailability.get().getId(), mentor.getId());

                Tutoring newTutoring = Tutoring.builder()
                                .mentor(mentor)
                                .discipline(discipline)
                                .tutoringClassType(request.tutoringClassType())
                                .status(StatusTutoring.PENDENTE)
                                .tutoringDate(request.tutoringDate())
                                .startTime(request.startTime())
                                .endTime(request.endTime())
                                .isChatEnable(false)
                                .topics(new HashSet<>())
                                .build();

                Tutoring savedTutoring = tutoringRepository.save(newTutoring);
                log.info("Tutoring entity created successfully with ID: {}", savedTutoring.getId());

                TutoringParticipants firstParticipant = TutoringParticipants.builder()
                                .tutoring(savedTutoring)
                                .user(mentee)
                                .topic(request.topic())
                                .build();
                tutoringParticipantsRepository.save(firstParticipant);
                log.info("Mentee {} added as the first participant to tutoring {} with topic '{}'", mentee.getId(),
                                savedTutoring.getId(), request.topic());

                ScheduledTutoringRepresentation representation = tutoringMapper
                                .toScheduledTutoringRepresentation(savedTutoring);
                int qtdParticipants = tutoringParticipantsRepository.countByTutoringId(savedTutoring.getId());

                return new ScheduledTutoringRepresentation(
                                representation.id(),
                                representation.mentorId(),
                                representation.mentorName(),
                                representation.disciplineId(),
                                representation.disciplineName(),
                                representation.tutoringClassType(),
                                representation.status(),
                                representation.startTime(),
                                representation.endTime(),
                                representation.tutoringDate(),
                                representation.local(),
                                representation.linkVideo(),
                                representation.maxParticipants(),
                                qtdParticipants,
                                representation.isChatEnable(),
                                representation.topics());
        }

        @Override
        @Transactional
        public UserAvailabilityRepresentation addMentorAvailability(Long mentorId, UserAvailabilityRequest request)
                        throws UserNotFoundException, TutoringOperationException, DisciplineNotFoundException { // Adicionado
                                                                                                                // DisciplineNotFoundException
                log.info("TutoringCommandService: Attempting to add availability for mentor ID: {} with discipline ID: {}",
                                mentorId, request.disciplineId());

                if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
                        throw new TutoringOperationException(
                                        "O horário de início deve ser anterior e diferente ao horário de fim.");
                }

                User mentor = userQuery.getUserReferenceById(mentorId);
                Discipline disciplineRef = disciplineQuery.getReferenceById(request.disciplineId()); // Pode lançar
                                                                                                     // DisciplineNotFoundException

                if (mentor.getRole() == Role.USER) {
                        log.info("TutoringCommandService: Mentor ID {} is adding availability. Promoting to MENTOR role.",
                                        mentorId);
                        userCommand.promoteToMentor(mentorId); // Supondo que UserCommand.promoteToMentor existe e
                                                               // funciona
                        mentor = userQuery.getUserReferenceById(mentorId); // Re-fetch user to get updated role if
                                                                           // necessary
                }

                boolean exactAvailabilityExists = mentorAvailabilityRepository
                                .existsByUserAndDisciplineAndDayOfWeekAndStartTimeAndEndTime(
                                                mentor, disciplineRef, request.dayOfWeek(), request.startTime(),
                                                request.endTime());
                if (exactAvailabilityExists) {
                        log.warn("TutoringCommandService: Exact availability already exists for mentor ID {}, discipline ID {}, day {}, time {} - {}",
                                        mentorId, request.disciplineId(), request.dayOfWeek(), request.startTime(),
                                        request.endTime());
                        throw new TutoringOperationException("Esta disponibilidade exata já foi cadastrada.");
                }

                MentorAvailability newAvailability = MentorAvailability.builder()
                                .user(mentor)
                                .discipline(disciplineRef)
                                .dayOfWeek(request.dayOfWeek())
                                .startTime(request.startTime())
                                .endTime(request.endTime())
                                .tutoringClassType(request.tutoringClassType())
                                .isAvailable(false) // Nova disponibilidade começa como inativa por padrão
                                .build();

                MentorAvailability savedAvailability = mentorAvailabilityRepository.save(newAvailability);
                log.info("TutoringCommandService: Availability entity added successfully with ID: {} for mentor ID: {}",
                                savedAvailability.getId(), mentorId);

                return tutoringMapper.toUserAvailabilityRepresentation(savedAvailability);
        }

        @Override
        @Transactional
        public List<UserAvailabilityRepresentation> updateMentorAvailabilityStatus(Long mentorId, Long availabilityId,
                        UpdateAvailabilityStatusRequest request)
                        throws UserNotFoundException, AvailabilityNotFoundException, TutoringOperationException,
                        AccessDeniedException {
                log.info("TutoringCommandService: Attempting to update availability status for ID: {} (Mentor: {}) to {}",
                                availabilityId, mentorId, request.isAvailable());

                User mentor = userQuery.getUserReferenceById(mentorId); // Garante que o mentor exista

                MentorAvailability targetAvailability = mentorAvailabilityRepository.findById(availabilityId)
                                .orElseThrow(() -> {
                                        log.warn("TutoringCommandService: Update status failed: Availability not found with ID: {}",
                                                        availabilityId);
                                        return new AvailabilityNotFoundException(
                                                        "Horário de disponibilidade não encontrado com ID: "
                                                                        + availabilityId);
                                });

                if (!targetAvailability.getUser().getId().equals(mentor.getId())) {
                        log.warn("TutoringCommandService: Update status failed: Availability ID {} does not belong to mentor ID {}",
                                        availabilityId, mentorId);
                        throw new AccessDeniedException("Usuário não autorizado a modificar esta disponibilidade.");
                }

                if (targetAvailability.getIsAvailable().equals(request.isAvailable())) {
                        log.info("TutoringCommandService: Availability ID {} already has the requested status ({}). No changes made.",
                                        availabilityId, request.isAvailable());
                        List<MentorAvailability> currentAvailabilities = mentorAvailabilityRepository
                                        .findByUserId(mentorId);
                        return tutoringMapper.toUserAvailabilityRepresentationList(currentAvailabilities);
                }

                boolean newStatus = request.isAvailable();
                List<MentorAvailability> availabilitiesToSave = new ArrayList<>();

                if (newStatus) { // Se está ativando esta disponibilidade
                        log.debug("TutoringCommandService: Activating availability ID {}. Checking for conflicts for mentor ID {} on day {}",
                                        availabilityId, mentorId, targetAvailability.getDayOfWeek());

                        List<MentorAvailability> sameDayAvailabilities = mentorAvailabilityRepository
                                        .findByUserIdAndDayOfWeek(mentorId, targetAvailability.getDayOfWeek());

                        for (MentorAvailability otherAvailability : sameDayAvailabilities) {
                                if (otherAvailability.getId().equals(availabilityId)) { // Não comparar consigo mesma
                                        continue;
                                }
                                // Se a outra disponibilidade estiver ativa E houver sobreposição
                                if (Boolean.TRUE.equals(otherAvailability.getIsAvailable())
                                                && doesOverlap(targetAvailability, otherAvailability)) {
                                        log.debug("TutoringCommandService: Availability ID {} conflicts with target ID {}. Deactivating otherAvailability.",
                                                        otherAvailability.getId(), availabilityId);
                                        otherAvailability.setIsAvailable(false);
                                        availabilitiesToSave.add(otherAvailability);
                                }
                        }
                        targetAvailability.setIsAvailable(true); // Ativa a disponibilidade alvo
                } else { // Se está desativando esta disponibilidade
                        log.debug("TutoringCommandService: Deactivating availability ID {}", availabilityId);
                        targetAvailability.setIsAvailable(false);
                }
                availabilitiesToSave.add(targetAvailability); // Adiciona a disponibilidade alvo (com status atualizado)
                                                              // para salvar

                if (!availabilitiesToSave.isEmpty()) {
                        mentorAvailabilityRepository.saveAll(availabilitiesToSave);
                        log.info("TutoringCommandService: Saved {} availability status changes for mentor ID {}",
                                        availabilitiesToSave.size(), mentorId);
                }

                List<MentorAvailability> updatedMentorAvailabilities = mentorAvailabilityRepository
                                .findByUserId(mentorId);
                return tutoringMapper.toUserAvailabilityRepresentationList(updatedMentorAvailabilities);
        }

        @Override
        @Transactional
        public void deleteMentorAvailability(Long mentorId, Long availabilityId)
                        throws UserNotFoundException, AvailabilityNotFoundException, AccessDeniedException {
                log.info("Attempting to delete availability ID: {} for mentor ID: {}", availabilityId, mentorId);
                User mentor = userQuery.getUserReferenceById(mentorId);

                MentorAvailability availability = mentorAvailabilityRepository.findById(availabilityId)
                                .orElseThrow(() -> new AvailabilityNotFoundException(
                                                "Disponibilidade não encontrada com ID: " + availabilityId));

                if (!availability.getUser().getId().equals(mentor.getId())) {
                        log.warn("Access denied: Mentor ID {} attempting to delete availability ID {} which does not belong to them.",
                                        mentorId, availabilityId);
                        throw new AccessDeniedException("Usuário não autorizado a excluir esta disponibilidade.");
                }

                // Adicionar lógica para verificar se existem tutorias AGENDADAS ou EM_ANDAMENTO
                // vinculadas a esta disponibilidade específica (se aplicável).
                // Esta verificação pode ser complexa se as tutorias não estão diretamente
                // ligadas à 'MentorAvailability'
                // mas sim criadas com base nela. Se as tutorias são criadas com data/hora
                // exatas,
                // a exclusão da disponibilidade pode não afetá-las diretamente, mas pode
                // impedir futuros agendamentos.
                // Se uma tutoria FOI agendada usando este slot, e o slot é deletado,
                // a tutoria agendada permanece, mas o slot de disponibilidade original não
                // existe mais.
                // Considere as implicações para o negócio. Por ora, a deleção é direta.

                mentorAvailabilityRepository.delete(availability);
                log.info("Disponibilidade ID: {} do mentor ID: {} excluída com sucesso.", availabilityId, mentorId);
        }

        @Override
        @Transactional
        public TutoringRepresentation confirmAndUpdatetutoring(Long tutoringId, ConfirmTutoringRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException {
                log.info("Attempting to confirm or update details for tutoring ID: {} by user {}", tutoringId,
                                (authentication != null ? authentication.getName() : "UNKNOWN"));

                if (authentication == null) {
                        throw new AccessDeniedException("Autenticação necessária para esta operação.");
                }

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Confirm/Update tutoring failed: Tutoring not found with ID: {}",
                                                        tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                User currentMentor = tutoring.getMentor();
                if (currentMentor == null) {
                        log.error("Confirm/Update tutoring failed: Mentor reference is null for tutoring ID: {}",
                                        tutoringId);
                        throw new IllegalStateException("Inconsistência de dados: Mentor não associado à monitoria.");
                }
                if (!userDetails.getUsername().equals(currentMentor.getEmail())) {
                        log.warn("Confirm/Update tutoring failed: User {} is not the mentor ({}) for tutoring ID {}",
                                        userDetails.getUsername(), currentMentor.getEmail(), tutoringId);
                        throw new AccessDeniedException(
                                        "Usuário não autorizado a confirmar ou atualizar esta monitoria.");
                }

                if (tutoring.getStatus() != StatusTutoring.PENDENTE
                                && tutoring.getStatus() != StatusTutoring.AGENDADA) {
                        log.warn("Confirm/Update tutoring failed: Tutoring ID {} is not in PENDENTE or AGENDADA status (current: {})",
                                        tutoringId, tutoring.getStatus());
                        throw new TutoringOperationException(
                                        "A monitoria só pode ser confirmada/atualizada se estiver no status PENDENTE ou AGENDADA. Status atual: "
                                                        + tutoring.getStatus());
                }

                if (tutoring.getTutoringClassType() == TutoringClassType.ONLINE) {
                        if (!StringUtils.hasText(request.linkVideo())) {
                                log.warn("Confirm/Update tutoring failed: linkVideo is required for ONLINE class type. Tutoring ID: {}",
                                                tutoringId);
                                throw new TutoringOperationException(
                                                "Link do vídeo é obrigatório para monitorias online.");
                        }
                        tutoring.setLocal(null); // Garante que local seja nulo para online
                        tutoring.setLinkVideo(request.linkVideo());
                } else if (tutoring.getTutoringClassType() == TutoringClassType.PRESENCIAL) {
                        if (!StringUtils.hasText(request.local())) {
                                log.warn("Confirm/Update tutoring failed: local is required for PRESENCIAL class type. Tutoring ID: {}",
                                                tutoringId);
                                throw new TutoringOperationException(
                                                "Local é obrigatório para monitorias presenciais.");
                        }
                        tutoring.setLinkVideo(null); // Garante que linkVideo seja nulo para presencial
                        tutoring.setLocal(request.local());
                }

                tutoring.setMaxParticipants(request.maxParticipants());
                tutoring.setIsChatEnable(request.isChatEnable());

                if (tutoring.getStatus() == StatusTutoring.PENDENTE) {
                        tutoring.setStatus(StatusTutoring.AGENDADA);
                        log.info("Tutoring ID: {} confirmed by mentor {}. Status changed from PENDENTE to AGENDADA.",
                                        tutoringId, userDetails.getUsername());
                } else {
                        log.info("Tutoring ID: {} (status AGENDADA) details updated by mentor {}.", tutoringId,
                                        userDetails.getUsername());
                }

                Tutoring updatedTutoring = tutoringRepository.save(tutoring);
                // Re-fetch e enriquecer para garantir que qtdParticipants seja atualizado na
                // representação
                return tutoringQuery.findTutoringById(updatedTutoring.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "Falha ao buscar monitoria recém-atualizada: "
                                                                + updatedTutoring.getId()));
        }

        @Override
        @Transactional
        public TutoringRepresentation updateTutoringStatus(Long tutoringId, UpdateTutoringStatusRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException {
                if (authentication == null) {
                        throw new AccessDeniedException("Autenticação necessária para esta operação.");
                }
                String requestingUserEmail = authentication.getName();
                log.info("Attempting status update for tutoring ID: {} to {} by user {}", tutoringId, request.status(),
                                requestingUserEmail);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Update status failed: Tutoring not found with ID: {}", tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                User authenticatedUser = userQuery.findByEmail(requestingUserEmail)
                                .map(userRep -> userQuery.getUserReferenceById(userRep.id()))
                                .orElseThrow(() -> new UserNotFoundException(
                                                "Usuário autenticado não encontrado: " + requestingUserEmail));

                boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;
                boolean isMentorOfTutoring = tutoring.getMentor() != null
                                && tutoring.getMentor().getId().equals(authenticatedUser.getId());

                StatusTutoring newStatus = request.status();
                StatusTutoring currentStatus = tutoring.getStatus();
                LocalDate currentDate = LocalDate.now();
                LocalTime currentTime = LocalTime.now();

                if (isAdmin) {
                        log.info("Admin {} is updating status for tutoring ID {} from {} to {}", requestingUserEmail,
                                        tutoringId, currentStatus, newStatus);
                        Set<StatusTutoring> allowedTransitionsForCurrentStatus = MENTOR_ALLOWED_TRANSITIONS
                                        .getOrDefault(currentStatus, Set.of());
                        if (!allowedTransitionsForCurrentStatus.contains(newStatus)) {
                                log.warn("Admin {} attempted an invalid status transition for tutoring ID {} from {} to {}.",
                                                requestingUserEmail, tutoringId, currentStatus, newStatus);
                        }
                } else if (isMentorOfTutoring) {
                        Set<StatusTutoring> allowedTransitions = MENTOR_ALLOWED_TRANSITIONS.get(currentStatus);
                        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
                                log.warn("Mentor {} attempted an invalid status transition for tutoring ID {} from {} to {}.",
                                                requestingUserEmail, tutoringId, currentStatus, newStatus);
                                throw new TutoringOperationException(
                                                String.format("Como mentor, você não pode mudar o status de '%s' para '%s'.",
                                                                currentStatus, newStatus));
                        }

                        if (newStatus == StatusTutoring.EM_ANDAMENTO) {
                                if (currentStatus != StatusTutoring.AGENDADA) {
                                        throw new TutoringOperationException(
                                                        "Mentoria só pode iniciar se estiver AGENDADA.");
                                }
                                if (tutoring.getTutoringDate() == null || tutoring.getStartTime() == null
                                                || tutoring.getEndTime() == null) {
                                        throw new TutoringOperationException(
                                                        "Mentoria não possui data/horário definidos para iniciar.");
                                }
                                if (!tutoring.getTutoringDate().equals(currentDate)) {
                                        throw new TutoringOperationException(
                                                        "A mentoria só pode ser iniciada no dia agendado ("
                                                                        + tutoring.getTutoringDate().format(
                                                                                        TutoringMapper.DATE_FORMATTER)
                                                                        + ").");
                                }
                                if (currentTime.isBefore(tutoring.getStartTime())) {
                                        throw new TutoringOperationException(
                                                        "A mentoria ainda não começou. Início programado para "
                                                                        + tutoring.getStartTime().format(
                                                                                        TutoringMapper.TIME_FORMATTER)
                                                                        + ".");
                                }
                                if (currentTime.isAfter(tutoring.getEndTime())) {
                                        throw new TutoringOperationException("O horário da mentoria já terminou ("
                                                        + tutoring.getEndTime().format(TutoringMapper.TIME_FORMATTER)
                                                        + "). Não é possível iniciar.");
                                }
                        }

                        if (newStatus == StatusTutoring.CONCLUIDA) {
                                if (currentStatus != StatusTutoring.EM_ANDAMENTO) {
                                        throw new TutoringOperationException(
                                                        "Mentoria só pode ser concluída se estiver EM ANDAMENTO.");
                                }
                                if (tutoring.getTutoringDate() == null || tutoring.getEndTime() == null) {
                                        throw new TutoringOperationException(
                                                        "Mentoria não possui data/horário de término definidos para concluir.");
                                }

                                if (!tutoring.getTutoringDate().equals(currentDate)) {
                                        throw new TutoringOperationException(
                                                        "A mentoria só pode ser concluída no dia agendado ("
                                                                        + tutoring.getTutoringDate().format(
                                                                                        TutoringMapper.DATE_FORMATTER)
                                                                        + ").");
                                }
                                if (currentTime.isBefore(tutoring.getEndTime())) {
                                        throw new TutoringOperationException(
                                                        "A mentoria ainda não terminou. Término programado para "
                                                                        + tutoring.getEndTime().format(
                                                                                        TutoringMapper.TIME_FORMATTER)
                                                                        + ".");
                                }
                        }

                } else {
                        log.warn("User {} (not mentor or admin) attempted to update status for tutoring ID {}.",
                                        requestingUserEmail, tutoringId);
                        throw new AccessDeniedException("Usuário não autorizado para esta alteração de status.");
                }

                tutoring.setStatus(newStatus);
                Tutoring updatedTutoring = tutoringRepository.save(tutoring);
                log.info("Status for tutoring ID: {} updated from {} to {} by user {}", tutoringId, currentStatus,
                                newStatus, requestingUserEmail);

                return tutoringQuery.findTutoringById(updatedTutoring.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "Falha ao buscar monitoria recém-atualizada: "
                                                                + updatedTutoring.getId()));
        }

        @Override
        @Transactional
        public TutoringRepresentation addParticipant(Long tutoringId, AddParticipantRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, UserNotFoundException, TutoringOperationException,
                        AccessDeniedException {
                if (authentication == null) {
                        throw new AccessDeniedException("Autenticação necessária para esta operação.");
                }
                String requestingUserEmail = authentication.getName();
                User authenticatedUser = userQuery.findByEmail(requestingUserEmail)
                                .map(rep -> userQuery.getUserReferenceById(rep.id()))
                                .orElseThrow(() -> new UserNotFoundException(
                                                "Usuário autenticado (" + requestingUserEmail + ") não encontrado."));

                // Verifica se o usuário que está tentando adicionar é o mesmo do request ou um
                // admin
                if (!authenticatedUser.getId().equals(request.userId()) && authenticatedUser.getRole() != Role.ADMIN) {
                        log.warn("User {} (authenticated) attempted to add a different user ID {} to tutoring {} without admin rights.",
                                        requestingUserEmail, request.userId(), tutoringId);
                        throw new AccessDeniedException(
                                        "Você não tem permissão para adicionar outro usuário a esta mentoria.");
                }

                log.info("User {} (authenticated as {}) attempting to add participant with ID {} to tutoring ID {}",
                                requestingUserEmail, authenticatedUser.getRole(), request.userId(), tutoringId);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Add participant failed: Tutoring not found with ID: {}", tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                // Verifica se a mentoria está em um status que permite adicionar participantes
                if (tutoring.getStatus() != StatusTutoring.AGENDADA
                                && tutoring.getStatus() != StatusTutoring.PENDENTE
                                && tutoring.getStatus() != StatusTutoring.EM_ANDAMENTO) {
                        log.warn("Add participant failed: Tutoring ID {} is not in AGENDADA, PENDENTE  or EM ANDAMENTO status (current: {}).",
                                        tutoringId, tutoring.getStatus());
                        // Mensagem de erro ajustada para refletir a lógica
                        throw new TutoringOperationException(
                                        "Não é possível se inscrever em uma monitoria que não está agendada ou pendente de confirmação.");
                }

                User participantUser = userQuery.getUserReferenceById(request.userId());

                // **** VALIDAÇÃO PRINCIPAL: Impede que o mentor da tutoria se adicione como
                // participante ****
                if (tutoring.getMentor() != null && tutoring.getMentor().getId().equals(participantUser.getId())) {
                        log.warn("Add participant failed: Mentor (ID: {}) cannot be added as a participant to their own tutoring (ID: {}).",
                                        participantUser.getId(), tutoringId);
                        throw new TutoringOperationException(
                                        "O mentor da monitoria não pode se inscrever como participante na própria monitoria.");
                }
                // **** FIM DA VALIDAÇÃO PRINCIPAL ****

                List<StatusTutoring> statusesToCheckForConflict = Arrays.asList(
                                StatusTutoring.PENDENTE, StatusTutoring.AGENDADA, StatusTutoring.EM_ANDAMENTO);
                checkUserConflictsWithExistingTutorings(participantUser, tutoring.getTutoringDate(),
                                tutoring.getStartTime(), tutoring.getEndTime(), statusesToCheckForConflict,
                                "participante");

                Integer maxParticipants = tutoring.getMaxParticipants();
                long currentParticipantsCount = tutoringParticipantsRepository.countByTutoringId(tutoringId);

                if (maxParticipants != null && currentParticipantsCount >= maxParticipants) {
                        log.warn("Add participant failed: Tutoring ID {} is full (max: {}, current: {}).", tutoringId,
                                        maxParticipants, currentParticipantsCount);
                        throw new TutoringOperationException("Monitoria lotada. Não há mais vagas disponíveis.");
                }

                boolean alreadyParticipating = tutoringParticipantsRepository.existsByTutoringIdAndUserId(tutoringId,
                                request.userId());
                if (alreadyParticipating) {
                        log.warn("Add participant failed: User ID {} is already participating in tutoring ID {}.",
                                        request.userId(), tutoringId);
                        throw new TutoringOperationException("Você já está inscrito nesta monitoria.");
                }

                TutoringParticipants newParticipant = TutoringParticipants.builder()
                                .tutoring(tutoring)
                                .user(participantUser)
                                .topic(request.topic())
                                .build();

                tutoringParticipantsRepository.save(newParticipant);
                log.info("User ID {} successfully added as participant to tutoring ID {} with topic '{}'",
                                request.userId(), tutoringId, request.topic());

                return tutoringQuery.findTutoringById(tutoringId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Falha ao buscar monitoria recém-atualizada após adicionar participante: "
                                                                + tutoringId));
        }

        @Override
        @Transactional
        public TutoringRatingRepresentation addTutoringRating(Long tutoringId, TutoringRatingRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException,
                        UserNotFoundException {
                if (authentication == null) {
                        throw new AccessDeniedException("Autenticação necessária para esta operação.");
                }
                String requestingUserEmail = authentication.getName();
                log.info("User {} attempting to add rating for tutoring ID: {}", requestingUserEmail, tutoringId);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Add rating failed: Tutoring not found with ID: {}", tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                if (tutoring.getStatus() != StatusTutoring.CONCLUIDA) {
                        log.warn("Add rating failed: Tutoring ID {} is not CONCLUIDA (current: {})", tutoringId,
                                        tutoring.getStatus());
                        throw new TutoringOperationException("Só é possível avaliar mentorias concluídas.");
                }

                User rater = userQuery.findByEmail(requestingUserEmail)
                                .map(userRep -> userQuery.getUserReferenceById(userRep.id()))
                                .orElseThrow(() -> {
                                        log.error("Add rating failed: Authenticated user {} not found in DB.",
                                                        requestingUserEmail);
                                        return new UserNotFoundException(
                                                        "Usuário autenticado não encontrado: " + requestingUserEmail);
                                });

                boolean isParticipant = tutoringParticipantsRepository.existsByTutoringIdAndUserId(tutoringId,
                                rater.getId());

                if (!isParticipant) {
                        log.warn("Add rating failed: User {} is not a participant of tutoring ID {}",
                                        requestingUserEmail, tutoringId);
                        throw new AccessDeniedException(
                                        "Usuário não autorizado a avaliar esta monitoria (não participou).");
                }

                if (tutoring.getMentor() != null && tutoring.getMentor().getId().equals(rater.getId())) {
                        log.warn("Add rating failed: Mentor (ID: {}) cannot rate their own tutoring (ID: {}).",
                                        rater.getId(), tutoringId);
                        throw new AccessDeniedException("O mentor não pode avaliar a própria monitoria.");
                }

                if (tutoring.getRating() != null) {
                        log.warn("Add rating failed: Tutoring ID {} already has a rating (Rating ID: {}).", tutoringId,
                                        tutoring.getRating().getId());
                        throw new TutoringOperationException("Esta monitoria já foi avaliada.");
                }

                TutoringRating newRating = TutoringRating.builder()
                                .tutoring(tutoring)
                                .mentorRating(request.mentorRating())
                                .review(request.review())
                                .build();

                TutoringRating savedRating = tutoringRatingRepository.save(newRating);
                log.info("Rating added successfully with ID {} for tutoring ID {}", savedRating.getId(), tutoringId);

                return tutoringQuery.mapToRatingRepresentation(savedRating, rater.getId());
        }

        @Override
        @Transactional
        public void deleteOrCancelTutoring(Long tutoringId, Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException {
                if (authentication == null) {
                        log.warn("Delete/Cancel tutoring failed: Authentication object is null for tutoring ID {}",
                                        tutoringId);
                        throw new AccessDeniedException("Autenticação é necessária para esta operação.");
                }
                String requestingUser = authentication.getName();
                log.info("Attempting to delete or cancel tutoring ID: {} requested by user {}", tutoringId,
                                requestingUser);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Delete/Cancel tutoring failed: Tutoring not found with ID: {}",
                                                        tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                User authenticatedUser = userQuery.findByEmail(requestingUser)
                                .map(userRep -> userQuery.getUserReferenceById(userRep.id()))
                                .orElseThrow(() -> new UserNotFoundException(
                                                "Usuário autenticado (" + requestingUser + ") não encontrado."));

                boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;
                boolean isMentorOfTutoring = tutoring.getMentor() != null
                                && tutoring.getMentor().getId().equals(authenticatedUser.getId());

                if (!isAdmin && !isMentorOfTutoring) {
                        log.warn("User {} is not authorized to delete/cancel tutoring ID {}", requestingUser,
                                        tutoringId);
                        throw new AccessDeniedException("Usuário não autorizado a excluir ou cancelar esta monitoria.");
                }

                if (tutoring.getStatus() == StatusTutoring.CONCLUIDA
                                || tutoring.getStatus() == StatusTutoring.CANCELADA) {
                        log.warn("User {} attempted to delete/cancel tutoring ID {} with status {}, which is not allowed.",
                                        requestingUser, tutoringId, tutoring.getStatus());
                        throw new TutoringOperationException("Monitoria não pode ser alterada pois seu status é "
                                        + tutoring.getStatus().toString().toLowerCase() + ".");
                }

                if (tutoring.getStatus() == StatusTutoring.PENDENTE) {
                        tutoringParticipantsRepository.deleteAllByTutoringId(tutoringId);
                        tutoringRepository.deleteById(tutoringId);
                        log.info("Tutoring ID: {} with status PENDENTE successfully deleted by user {}", tutoringId,
                                        requestingUser);
                } else { // AGENDADA ou EM_ANDAMENTO
                        tutoring.setStatus(StatusTutoring.CANCELADA);
                        tutoringRepository.save(tutoring);
                        log.info("Tutoring ID: {} status changed to CANCELADA by user {}", tutoringId, requestingUser);
                        // Adicional: notificar participantes sobre o cancelamento
                }
        }

        @Override
        @Transactional
        public void deleteTutoringRating(Long ratingId) throws TutoringOperationException {
                log.info("Attempting to delete tutoring rating with ID: {}", ratingId);
                if (!tutoringRatingRepository.existsById(ratingId)) {
                        log.warn("Delete rating failed: Rating not found with ID: {}", ratingId);
                        throw new TutoringOperationException("Avaliação não encontrada com ID: " + ratingId);
                }
                tutoringRatingRepository.deleteById(ratingId);
                log.info("Tutoring rating with ID: {} deleted successfully.", ratingId);
        }

        @Override
        @Transactional
        public void removeParticipant(Long tutoringId, Long userId, Authentication authentication)
                        throws TutoringNotFoundException, UserNotFoundException, TutoringOperationException,
                        AccessDeniedException {
                if (authentication == null) {
                        throw new AccessDeniedException("Autenticação necessária para esta operação.");
                }
                String requestingUserEmail = authentication.getName();
                log.info("Service: Usuário {} tentando remover participante com ID {} da mentoria ID {}",
                                requestingUserEmail, userId, tutoringId);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Service: Remoção de participante falhou: Mentoria não encontrada com ID: {}",
                                                        tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                User userToRemove = userQuery.getUserReferenceById(userId); // Garante que o usuário a ser removido
                                                                            // exista

                User authenticatedUser = userQuery.findByEmail(requestingUserEmail)
                                .map(rep -> userQuery.getUserReferenceById(rep.id()))
                                .orElseThrow(() -> new UserNotFoundException("Usuário autenticado não encontrado."));

                boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;
                boolean isSelfRemoving = authenticatedUser.getId().equals(userId);
                boolean isMentorOfTutoringRemovingParticipant = tutoring.getMentor() != null
                                && tutoring.getMentor().getId().equals(authenticatedUser.getId());

                // Permite se: é o próprio usuário saindo, OU é admin, OU é o mentor da tutoria
                // removendo um participante
                if (!isSelfRemoving && !isAdmin && !isMentorOfTutoringRemovingParticipant) {
                        log.warn("Service: Usuário {} não autorizado a remover participante {} da mentoria {}",
                                        requestingUserEmail, userId, tutoringId);
                        throw new AccessDeniedException("Você não tem permissão para remover este participante.");
                }

                if (tutoring.getMentor() != null && tutoring.getMentor().getId().equals(userId)) {
                        log.warn("Service: Tentativa de remover o mentor (ID: {}) como participante da mentoria ID: {}. Mentores devem usar 'cancelar mentoria'.",
                                        userId, tutoringId);
                        throw new TutoringOperationException(
                                        "O mentor não pode ser removido como participante. Para cancelar a mentoria, utilize a opção apropriada.");
                }

                TutoringParticipants participantToRemove = tutoringParticipantsRepository
                                .findByTutoringIdAndUserId(tutoringId, userId)
                                .orElseThrow(() -> {
                                        log.warn("Service: Usuário ID {} não é um participante da mentoria ID {}",
                                                        userId, tutoringId);
                                        return new TutoringOperationException(
                                                        "Usuário não é participante desta monitoria.");
                                });

                if (tutoring.getStatus() == StatusTutoring.PENDENTE
                                || tutoring.getStatus() == StatusTutoring.AGENDADA) {
                        tutoringParticipantsRepository.delete(participantToRemove);
                        long remainingParticipants = tutoringParticipantsRepository.countByTutoringId(tutoringId);

                        // Se era o último participante de uma mentoria PENDENTE (que ainda não foi
                        // confirmada pelo mentor),
                        // a mentoria pode ser cancelada/excluída.
                        if (tutoring.getStatus() == StatusTutoring.PENDENTE && remainingParticipants == 0) {
                                log.info("Service: Mentoria PENDENTE ID {} ficou sem participantes após saída do usuário ID {}. Cancelando mentoria.",
                                                tutoringId, userId);
                                tutoring.setStatus(StatusTutoring.CANCELADA); // Ou deletar:
                                                                              // tutoringRepository.delete(tutoring);
                                tutoringRepository.save(tutoring);
                        }
                        log.info("Service: Participante ID {} removido com sucesso da mentoria ID {}", userId,
                                        tutoringId);
                } else {
                        log.warn("Service: Tentativa de sair/remover da mentoria ID {} com status inválido: {}",
                                        tutoringId, tutoring.getStatus());
                        throw new TutoringOperationException(
                                        "Não é possível sair/remover de uma mentoria que não esteja PENDENTE ou AGENDADA.");
                }
        }

        @Override
        @Transactional
        public TutoringRepresentation cancelMentorTutoring(Long userIdFromPath, Long tutoringId,
                        boolean deactivateAvailability, Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException,
                        UserNotFoundException {
                if (authentication == null) {
                        throw new AccessDeniedException("Autenticação necessária para esta operação.");
                }
                String authenticatedUserEmail = authentication.getName();
                log.info("Service: Authenticated user {} attempting to cancel tutoring ID: {} for user ID in path: {} with deactivateAvailability: {}",
                                authenticatedUserEmail, tutoringId, userIdFromPath, deactivateAvailability);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Service: Cancel tutoring failed. Tutoring not found with ID: {}",
                                                        tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                User authenticatedUser = userQuery.findByEmail(authenticatedUserEmail)
                                .map(rep -> userQuery.getUserReferenceById(rep.id()))
                                .orElseThrow(() -> {
                                        log.error("Service: Cancel tutoring failed. Authenticated user {} not found.",
                                                        authenticatedUserEmail);
                                        return new UserNotFoundException("Usuário autenticado não encontrado: "
                                                        + authenticatedUserEmail);
                                });

                User userFromPathAsMentor = userQuery.getUserReferenceById(userIdFromPath);

                boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;

                if (isAdmin) { // Admin está realizando a ação
                        if (tutoring.getMentor() == null
                                        || !tutoring.getMentor().getId().equals(userFromPathAsMentor.getId())) {
                                log.warn("Service (Admin Action): User ID in path {} is not the mentor of tutoring ID {}. Actual mentor is {}.",
                                                userFromPathAsMentor.getId(), tutoringId,
                                                (tutoring.getMentor() != null ? tutoring.getMentor().getId() : "null"));
                                throw new AccessDeniedException("O usuário (ID: " + userFromPathAsMentor.getId()
                                                + ") especificado no caminho não é o mentor desta monitoria (ID: "
                                                + tutoringId + ").");
                        }
                        log.info("Service: Admin {} is performing cancellation for mentor {}'s tutoring {}.",
                                        authenticatedUserEmail, userFromPathAsMentor.getEmail(), tutoringId);
                } else { // Usuário não-admin (deve ser o próprio mentor)
                        if (!authenticatedUser.getId().equals(userFromPathAsMentor.getId())) {
                                log.warn("Service: Authenticated user {} (ID: {}) is not authorized to cancel tutoring for user ID in path {} (not an Admin).",
                                                authenticatedUserEmail, authenticatedUser.getId(),
                                                userFromPathAsMentor.getId());
                                throw new AccessDeniedException(
                                                "Você não tem permissão para realizar esta ação para o usuário especificado (ID: "
                                                                + userFromPathAsMentor.getId() + ").");
                        }
                        if (tutoring.getMentor() == null
                                        || !tutoring.getMentor().getId().equals(authenticatedUser.getId())) {
                                log.warn("Service: Authenticated user {} (ID: {}) is not the mentor of tutoring ID {}. Actual mentor is {}.",
                                                authenticatedUserEmail, authenticatedUser.getId(), tutoringId,
                                                (tutoring.getMentor() != null ? tutoring.getMentor().getId() : "null"));
                                throw new AccessDeniedException(
                                                "Você não é o mentor desta monitoria (ID: " + tutoringId + ").");
                        }
                        log.info("Service: Mentor {} is performing cancellation for their tutoring {}.",
                                        authenticatedUserEmail, tutoringId);
                }

                if (tutoring.getStatus() == StatusTutoring.CONCLUIDA
                                || tutoring.getStatus() == StatusTutoring.CANCELADA) {
                        log.warn("Service: Tutoring ID {} cannot be cancelled. Status is already {} or {}.", tutoringId,
                                        StatusTutoring.CONCLUIDA, StatusTutoring.CANCELADA);
                        throw new TutoringOperationException("Monitoria não pode ser cancelada pois já está "
                                        + tutoring.getStatus().toString().toLowerCase() + ".");
                }

                tutoring.setStatus(StatusTutoring.CANCELADA);
                Tutoring cancelledTutoring = tutoringRepository.save(tutoring);
                log.info("Service: Tutoring ID {} status changed to CANCELADA by authenticated user {} (acting for/as mentor {}).",
                                tutoringId, authenticatedUserEmail, userFromPathAsMentor.getEmail());

                if (deactivateAvailability) {
                        log.info("Service: Attempting to deactivate corresponding availability for tutoring ID {}.",
                                        tutoringId);
                        try {
                                User mentorOfTutoring = tutoring.getMentor(); // Este é o userFromPathAsMentor validado
                                Discipline discipline = tutoring.getDiscipline();
                                DayWeek dayOfWeek = mapJavaDayOfWeekToCustom(tutoring.getTutoringDate().getDayOfWeek());

                                Optional<MentorAvailability> coveringAvailabilityOpt = mentorAvailabilityRepository
                                                .findCoveringAndActiveAvailability(
                                                                mentorOfTutoring,
                                                                discipline,
                                                                dayOfWeek,
                                                                tutoring.getStartTime(),
                                                                tutoring.getEndTime());

                                if (coveringAvailabilityOpt.isPresent()) {
                                        MentorAvailability availabilityToDeactivate = coveringAvailabilityOpt.get();
                                        if (Boolean.TRUE.equals(availabilityToDeactivate.getIsAvailable())) {
                                                UpdateAvailabilityStatusRequest updateReq = new UpdateAvailabilityStatusRequest(
                                                                false);
                                                try {
                                                        // Chamando o método do próprio serviço para manter a lógica de
                                                        // conflito centralizada
                                                        updateMentorAvailabilityStatus(mentorOfTutoring.getId(),
                                                                        availabilityToDeactivate.getId(), updateReq);
                                                        log.info("Service: Availability ID {} for mentor {} successfully deactivated via command due to tutoring {} cancellation.",
                                                                        availabilityToDeactivate.getId(),
                                                                        mentorOfTutoring.getId(), tutoringId);
                                                } catch (Exception e) {
                                                        log.error("Service: Error deactivating availability ID {} via command: {}",
                                                                        availabilityToDeactivate.getId(),
                                                                        e.getMessage(), e);
                                                }
                                        } else {
                                                log.info("Service: Availability ID {} for mentor {} was already inactive. No change made.",
                                                                availabilityToDeactivate.getId(),
                                                                mentorOfTutoring.getId());
                                        }
                                } else {
                                        log.warn("Service: No specific active and covering availability found for mentor {} on {} for discipline '{}' matching tutoring {} time. Availability not deactivated.",
                                                        mentorOfTutoring.getId(), dayOfWeek,
                                                        (discipline != null ? discipline.getDisciplineName() : "N/A"),
                                                        tutoringId);
                                }
                        } catch (Exception e) {
                                log.error("Service: Error occurred while trying to deactivate availability for tutoring ID {}: {}",
                                                tutoringId, e.getMessage(), e);
                        }
                }
                return tutoringQuery.findTutoringById(cancelledTutoring.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "Falha ao buscar representação da monitoria recém-cancelada: "
                                                                + cancelledTutoring.getId()));
        }

        // --- Métodos auxiliares (já presentes no seu código) ---
        private boolean doesOverlap(MentorAvailability target, MentorAvailability other) {
                if (target == null || other == null || target.getStartTime() == null || target.getEndTime() == null ||
                                other.getStartTime() == null || other.getEndTime() == null) {
                        return false; // Não pode haver sobreposição se algum horário for nulo
                }
                LocalTime targetStart = target.getStartTime();
                LocalTime targetEnd = target.getEndTime();
                LocalTime otherStart = other.getStartTime();
                LocalTime otherEnd = other.getEndTime();

                // Verifica se targetStart < otherEnd E targetEnd > otherStart
                return targetStart.isBefore(otherEnd) && targetEnd.isAfter(otherStart);
        }

        private DayWeek mapJavaDayOfWeekToCustom(java.time.DayOfWeek javaDayOfWeek) {
                if (javaDayOfWeek == null) {
                        throw new IllegalArgumentException("java.time.DayOfWeek não pode ser nulo para mapeamento.");
                }
                // Adaptação para garantir que os nomes dos enums correspondam se forem
                // diferentes.
                // Se forem idênticos (ex: MONDAY -> SEGUNDA_FEIRA), o valueOf funciona.
                return switch (javaDayOfWeek) {
                        case MONDAY -> DayWeek.SEGUNDA_FEIRA;
                        case TUESDAY -> DayWeek.TERCA_FEIRA;
                        case WEDNESDAY -> DayWeek.QUARTA_FEIRA;
                        case THURSDAY -> DayWeek.QUINTA_FEIRA;
                        case FRIDAY -> DayWeek.SEXTA_FEIRA;
                        case SATURDAY -> DayWeek.SABADO;
                        case SUNDAY -> DayWeek.DOMINGO;
                };
        }

        private void checkUserConflictsWithExistingTutorings(User user, LocalDate date, LocalTime startTime,
                        LocalTime endTime, List<StatusTutoring> statusesToConsider, String userRole)
                        throws TutoringOperationException {
                log.debug("Checking time conflicts for {} (ID: {}) on {} between {} and {} against existing tutorings with statuses: {}",
                                userRole, user.getId(), date, startTime, endTime, statusesToConsider);
                List<Tutoring> conflictingTutorings = tutoringRepository
                                .findConflictingTutoringsForUserWithSpecificStatuses(user, date, startTime, endTime,
                                                statusesToConsider);
                if (!conflictingTutorings.isEmpty()) {
                        Tutoring conflict = conflictingTutorings.get(0);
                        String conflictMenteeName = "[Mentorado não carregado]";
                        if (conflict.getTopics() != null && !conflict.getTopics().isEmpty()) {
                                Optional<User> firstMenteeUserOpt = conflict.getTopics().stream().findFirst()
                                                .map(TutoringParticipants::getUser);
                                if (firstMenteeUserOpt.isPresent()) {
                                        User firstMenteeUser = firstMenteeUserOpt.get();
                                        conflictMenteeName = firstMenteeUser.getFirstName() + " "
                                                        + firstMenteeUser.getLastName();
                                }
                        } else if (conflict.getMentor() != null && conflict.getMentor().getId().equals(user.getId())) {
                                return;
                        }

                        String withWhom = "[Mentor não carregado]";
                        if (conflict.getMentor() != null) {
                                withWhom = conflict.getMentor().getId().equals(user.getId())
                                                ? "com " + conflictMenteeName
                                                : "com o mentor " + conflict.getMentor().getFirstName() + " "
                                                                + conflict.getMentor().getLastName();
                        }

                        String disciplineNameStr = "[Disciplina não carregada]";
                        if (conflict.getDiscipline() != null) {
                                disciplineNameStr = conflict.getDiscipline().getDisciplineName();
                        }

                        String conflictDetails = String.format("Mentoria ID %d (%s - %s) de '%s' %s.", conflict.getId(),
                                        (conflict.getStartTime() != null
                                                        ? conflict.getStartTime().format(TutoringMapper.TIME_FORMATTER)
                                                        : "N/A"),
                                        (conflict.getEndTime() != null
                                                        ? conflict.getEndTime().format(TutoringMapper.TIME_FORMATTER)
                                                        : "N/A"),
                                        disciplineNameStr,
                                        withWhom);
                        log.warn("Schedule/Participation failed: {} (ID: {}) has a time conflict. Conflicting tutoring details: {}",
                                        userRole, user.getId(), conflictDetails);
                        throw new TutoringOperationException(String.format(
                                        "O %s (você) já possui uma mentoria (%s) que conflita com este horário (%s das %s às %s). Detalhe do conflito: %s",
                                        userRole,
                                        disciplineNameStr,
                                        (date != null ? date.format(TutoringMapper.DATE_FORMATTER) : "N/A"),
                                        (startTime != null ? startTime.format(TutoringMapper.TIME_FORMATTER) : "N/A"),
                                        (endTime != null ? endTime.format(TutoringMapper.TIME_FORMATTER) : "N/A"),
                                        conflictDetails));
                }
                log.debug("No time conflicts found with existing tutorings for {} (ID: {})", userRole, user.getId());
        }
}