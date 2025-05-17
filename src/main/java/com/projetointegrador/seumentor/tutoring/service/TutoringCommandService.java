// src/main/java/com/projetointegrador/seumentor/tutoring/service/TutoringCommandService.java
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

// import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
// import java.util.stream.Collectors;

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
                                request.endTime(),
                                statusesToCheckForConflict, "mentorado");
                checkUserConflictsWithExistingTutorings(mentor, request.tutoringDate(), request.startTime(),
                                request.endTime(),
                                statusesToCheckForConflict, "mentor");

                DayWeek dayOfWeek = mapJavaDayOfWeekToCustom(request.tutoringDate().getDayOfWeek()); // Método auxiliar
                                                                                                     // permanece
                Optional<MentorAvailability> coveringAvailability = mentorAvailabilityRepository
                                .findCoveringAndActiveAvailability(
                                                mentor,
                                                discipline,
                                                dayOfWeek,
                                                request.startTime(),
                                                request.endTime());

                if (coveringAvailability.isEmpty()) {
                        log.warn(
                                        "Schedule failed: Mentor (ID: {}) does not have an active and covering availability for discipline '{}' on {} from {} to {}.",
                                        mentor.getId(), discipline.getDisciplineName(), dayOfWeek,
                                        request.startTime().format(TutoringMapper.TIME_FORMATTER), // Usar formatter do
                                                                                                   // Mapper
                                        request.endTime().format(TutoringMapper.TIME_FORMATTER)); // Usar formatter do
                                                                                                  // Mapper
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

                // Usar o mapper
                ScheduledTutoringRepresentation representation = tutoringMapper
                                .toScheduledTutoringRepresentation(savedTutoring);

                // Preencher qtdParticipants após o mapeamento
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
                                qtdParticipants, // Preenchido aqui
                                representation.isChatEnable(),
                                representation.topics());
        }

        @Override
        @Transactional
        public UserAvailabilityRepresentation addMentorAvailability(Long mentorId, UserAvailabilityRequest request)
                        throws UserNotFoundException, TutoringOperationException {
                log.info("TutoringCommandService: Attempting to add availability for mentor ID: {} with discipline ID: {}",
                                mentorId, request.disciplineId());

                if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
                        throw new TutoringOperationException(
                                        "O horário de início deve ser anterior e diferente ao horário de fim.");
                }

                User mentor = userQuery.getUserReferenceById(mentorId);
                Discipline disciplineRef = disciplineQuery.getReferenceById(request.disciplineId());

                if (mentor.getRole() == Role.USER) {
                        log.info("TutoringCommandService: Mentor ID {} is adding availability. Promoting to MENTOR role.",
                                        mentorId);
                        userCommand.promoteToMentor(mentorId);
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
                                .isAvailable(false)
                                .build();

                MentorAvailability savedAvailability = mentorAvailabilityRepository.save(newAvailability);
                log.info("TutoringCommandService: Availability entity added successfully with ID: {} for mentor ID: {}",
                                savedAvailability.getId(), mentorId);

                return tutoringMapper.toUserAvailabilityRepresentation(savedAvailability); // Usar o mapper
        }

        @Override
        @Transactional
        public List<UserAvailabilityRepresentation> updateMentorAvailabilityStatus(Long mentorId, Long availabilityId,
                        UpdateAvailabilityStatusRequest request)
                        throws UserNotFoundException, AvailabilityNotFoundException, TutoringOperationException,
                        AccessDeniedException {
                log.info("TutoringCommandService: Attempting to update availability status for ID: {} (Mentor: {}) to {}",
                                availabilityId, mentorId, request.isAvailable());

                User mentor = userQuery.getUserReferenceById(mentorId);

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
                        return tutoringMapper.toUserAvailabilityRepresentationList(currentAvailabilities); // Usar o
                                                                                                           // mapper
                }

                boolean newStatus = request.isAvailable();
                List<MentorAvailability> availabilitiesToSave = new ArrayList<>();

                if (newStatus) {
                        log.debug("TutoringCommandService: Activating availability ID {}. Checking for conflicts for mentor ID {} on day {}",
                                        availabilityId, mentorId, targetAvailability.getDayOfWeek());

                        List<MentorAvailability> sameDayAvailabilities = mentorAvailabilityRepository
                                        .findByUserIdAndDayOfWeek(
                                                        mentorId, targetAvailability.getDayOfWeek());

                        for (MentorAvailability otherAvailability : sameDayAvailabilities) {
                                if (otherAvailability.getId().equals(availabilityId))
                                        continue;

                                if (Boolean.TRUE.equals(otherAvailability.getIsAvailable())
                                                && doesOverlap(targetAvailability, otherAvailability)) {
                                        log.debug("TutoringCommandService: Availability ID {} conflicts with target ID {}. Deactivating otherAvailability.",
                                                        otherAvailability.getId(), availabilityId);
                                        otherAvailability.setIsAvailable(false);
                                        availabilitiesToSave.add(otherAvailability);
                                }
                        }
                        targetAvailability.setIsAvailable(true);
                } else {
                        log.debug("TutoringCommandService: Deactivating availability ID {}", availabilityId);
                        targetAvailability.setIsAvailable(false);
                }
                availabilitiesToSave.add(targetAvailability);

                if (!availabilitiesToSave.isEmpty()) {
                        mentorAvailabilityRepository.saveAll(availabilitiesToSave);
                        log.info("TutoringCommandService: Saved {} availability status changes for mentor ID {}",
                                        availabilitiesToSave.size(), mentorId);
                }

                List<MentorAvailability> updatedMentorAvailabilities = mentorAvailabilityRepository
                                .findByUserId(mentorId);
                return tutoringMapper.toUserAvailabilityRepresentationList(updatedMentorAvailabilities); // Usar o
                                                                                                         // mapper
        }

        // Os métodos mapToScheduledTutoringRepresentation e
        // mapToUserAvailabilityRepresentation foram removidos
        // pois suas funcionalidades agora são cobertas pelo TutoringMapper.

        // Métodos auxiliares como checkUserConflictsWithExistingTutorings,
        // mapJavaDayOfWeekToCustom, doesOverlap permanecem.
        // Ajustar chamadas de formatação de data/hora nos logs ou mensagens de exceção
        // se necessário,
        // para usar TutoringMapper.TIME_FORMATTER ou TutoringMapper.DATE_FORMATTER se
        // você os expôs como públicos estáticos,
        // ou recriá-los como privados estáticos aqui se preferir não expô-los no
        // mapper.
        // Por ora, a formatação em checkUserConflictsWithExistingTutorings foi ajustada
        // para usar os formatters do TutoringMapper.

        // ... (resto dos métodos como: confirmAndUpdatetutoring, updateTutoringStatus,
        // addParticipant, etc.)
        // Assegure-se que eles também não estejam fazendo mapeamentos manuais se o
        // TutoringMapper puder cobri-los.
        // Por exemplo, o retorno de confirmAndUpdatetutoring é TutoringRepresentation.
        // Se o TutoringMapper
        // tiver um método para `Tutoring -> TutoringRepresentation`, ele deve ser
        // usado.
        // O mesmo para `updateTutoringStatus`, `addParticipant`,
        // `cancelMentorTutoring`.
        // `addTutoringRating` retorna `TutoringRatingRepresentation`, então precisaria
        // de um mapeamento para isso também.

        // --- Implementação dos métodos restantes de TutoringCommand ---
        // (Mantendo a estrutura dos métodos já fornecidos e corrigidos anteriormente)

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
                        tutoring.setLocal(null);
                        tutoring.setLinkVideo(request.linkVideo());
                } else if (tutoring.getTutoringClassType() == TutoringClassType.PRESENCIAL) {
                        if (!StringUtils.hasText(request.local())) {
                                log.warn("Confirm/Update tutoring failed: local is required for PRESENCIAL class type. Tutoring ID: {}",
                                                tutoringId);
                                throw new TutoringOperationException(
                                                "Local é obrigatório para monitorias presenciais.");
                        }
                        tutoring.setLinkVideo(null);
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
                // Assumindo que TutoringQuery tem um método para mapear Tutoring para
                // TutoringRepresentation
                // ou que TutoringMapper será expandido para isso.
                // Por enquanto, vou depender do tutoringQuery.findTutoringById que retorna
                // TutoringRepresentation
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

                if (!isAdmin && !isMentorOfTutoring) {
                        throw new AccessDeniedException("Usuário não autorizado para esta alteração de status.");
                }

                if (isMentorOfTutoring && !isAdmin) {
                        if (request.status() == StatusTutoring.EM_ANDAMENTO
                                        && tutoring.getStatus() != StatusTutoring.AGENDADA) {
                                throw new TutoringOperationException("Mentoria só pode iniciar se estiver AGENDADA.");
                        }
                        if (request.status() == StatusTutoring.CONCLUIDA
                                        && tutoring.getStatus() != StatusTutoring.EM_ANDAMENTO) {
                                throw new TutoringOperationException(
                                                "Mentoria só pode ser concluída se estiver EM ANDAMENTO.");
                        }
                        if (request.status() == StatusTutoring.CANCELADA
                                        && !(tutoring.getStatus() == StatusTutoring.PENDENTE
                                                        || tutoring.getStatus() == StatusTutoring.AGENDADA
                                                        || tutoring.getStatus() == StatusTutoring.EM_ANDAMENTO)) {
                                throw new TutoringOperationException(
                                                "Mentoria só pode ser cancelada pelo mentor se estiver PENDENTE, AGENDADA ou EM ANDAMENTO.");
                        }
                }

                StatusTutoring newStatus = request.status();
                StatusTutoring currentStatus = tutoring.getStatus();
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
                log.info("User {} (authenticated) attempting to add participant with ID {} to tutoring ID {}",
                                requestingUserEmail, request.userId(), tutoringId);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Add participant failed: Tutoring not found with ID: {}", tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                if (tutoring.getStatus() != StatusTutoring.AGENDADA) {
                        log.warn("Add participant failed: Tutoring ID {} is not in AGENDADA status (current: {}).",
                                        tutoringId, tutoring.getStatus());
                        throw new TutoringOperationException(
                                        "Não é possível se inscrever em uma monitoria que não está agendada.");
                }

                User participantUser = userQuery.getUserReferenceById(request.userId());

                if (tutoring.getMentor() != null && tutoring.getMentor().getId().equals(participantUser.getId())) {
                        log.warn("Add participant failed: Mentor (ID: {}) cannot be added as a participant to their own tutoring (ID: {}).",
                                        participantUser.getId(), tutoringId);
                        throw new TutoringOperationException(
                                        "O mentor da monitoria não pode se inscrever como participante.");
                }

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
                        tutoringParticipantsRepository.deleteAllByTutoringId(tutoringId); // Usar um método de exclusão
                                                                                          // em lote
                        tutoringRepository.deleteById(tutoringId);
                        log.info("Tutoring ID: {} with status PENDENTE successfully deleted by user {}", tutoringId,
                                        requestingUser);
                } else {
                        tutoring.setStatus(StatusTutoring.CANCELADA);
                        tutoringRepository.save(tutoring);
                        log.info("Tutoring ID: {} status changed to CANCELADA by user {}", tutoringId, requestingUser);
                }
        }

        @Override
        @Transactional
        public void deleteTutoringRating(Long ratingId) throws TutoringOperationException {
                log.info("Attempting to delete tutoring rating with ID: {}", ratingId);
                TutoringRating rating = tutoringRatingRepository.findById(ratingId)
                                .orElseThrow(() -> {
                                        log.warn("Delete rating failed: Rating not found with ID: {}", ratingId);
                                        return new TutoringOperationException(
                                                        "Avaliação não encontrada com ID: " + ratingId);
                                });

                tutoringRatingRepository.deleteById(ratingId);
                log.info("Tutoring rating with ID: {} deleted successfully.", ratingId);
        }

        @Override
        @Transactional
        public void deleteMentorAvailability(Long mentorId, Long availabilityId)
                        throws UserNotFoundException, AvailabilityNotFoundException, TutoringOperationException,
                        AccessDeniedException {
                // TODO: Implement the logic to delete a mentor's availability.
                // 1. Find the mentor user by mentorId.
                // 2. Find the availability by availabilityId.
                // 3. Check if the availability belongs to the mentor.
                // 4. Check if there are any non-cancellable tutorias scheduled during this
                // availability.
                // If so, throw a TutoringOperationException or handle as per your business
                // logic.
                // 5. Delete the availability.
                log.info("Attempting to delete availability ID: {} for mentor ID: {}", availabilityId, mentorId);
                User mentor = userQuery.getUserReferenceById(mentorId); // Assuming userQuery can throw
                                                                        // UserNotFoundException

                MentorAvailability availability = mentorAvailabilityRepository.findById(availabilityId)
                                .orElseThrow(() -> new AvailabilityNotFoundException(
                                                "Disponibilidade não encontrada com ID: " + availabilityId));

                if (!availability.getUser().getId().equals(mentor.getId())) {
                        throw new AccessDeniedException("Usuário não autorizado a excluir esta disponibilidade.");
                }

                // Optional: Check for dependent tutorings before deleting.
                // Example: if
                // (tutoringRepository.existsByMentorAndAvailabilityAndStatusNotIn(mentor,
                // availability, Arrays.asList(StatusTutoring.CANCELADA,
                // StatusTutoring.CONCLUIDA))) {
                // throw new TutoringOperationException("Não é possível excluir disponibilidade
                // pois existem mentorias ativas ou pendentes associadas a ela.");
                // }

                mentorAvailabilityRepository.delete(availability);
                log.info("Disponibilidade ID: {} do mentor ID: {} excluída com sucesso.", availabilityId, mentorId);
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

                userQuery.getUserReferenceById(userId);

                User authenticatedUser = userQuery.findByEmail(requestingUserEmail)
                                .map(rep -> userQuery.getUserReferenceById(rep.id()))
                                .orElseThrow(() -> new UserNotFoundException("Usuário autenticado não encontrado."));

                boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;
                boolean isSelf = authenticatedUser.getId().equals(userId);
                boolean isMentorOfTutoring = tutoring.getMentor() != null
                                && tutoring.getMentor().getId().equals(authenticatedUser.getId());

                if (!isSelf && !isAdmin && !isMentorOfTutoring) {
                        log.warn("Service: Usuário {} não autorizado a remover participante {} da mentoria {}",
                                        requestingUserEmail, userId, tutoringId);
                        throw new AccessDeniedException("Você não tem permissão para remover este participante.");
                }

                if (tutoring.getMentor() != null && tutoring.getMentor().getId().equals(userId)) {
                        log.warn("Service: Tentativa de remover o mentor (ID: {}) como participante da mentoria ID: {}",
                                        userId, tutoringId);
                        throw new TutoringOperationException(
                                        "O mentor não pode ser removido como participante. Para cancelar a mentoria, use a opção apropriada.");
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

                        if (tutoring.getStatus() == StatusTutoring.PENDENTE && remainingParticipants == 0) {
                                log.info("Service: Mentoria PENDENTE ID {} ficou sem participantes após saída do usuário ID {}. Cancelando mentoria.",
                                                tutoringId, userId);
                                tutoring.setStatus(StatusTutoring.CANCELADA);
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

                if (isAdmin) {
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
                } else {
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
                                User mentorOfTutoring = tutoring.getMentor();
                                Discipline discipline = tutoring.getDiscipline();
                                DayWeek dayOfWeek = mapJavaDayOfWeekToCustom(tutoring.getTutoringDate().getDayOfWeek()); // Corrigido
                                                                                                                         // para
                                                                                                                         // pegar
                                                                                                                         // o
                                                                                                                         // dia
                                                                                                                         // da
                                                                                                                         // semana
                                                                                                                         // da
                                                                                                                         // data
                                                                                                                         // da
                                                                                                                         // tutoria

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
                                                        discipline.getDisciplineName(), tutoringId);
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

        // --- Métodos auxiliares ---
        private boolean doesOverlap(MentorAvailability target, MentorAvailability other) {
                if (target == null || other == null || target.getStartTime() == null || target.getEndTime() == null ||
                                other.getStartTime() == null || other.getEndTime() == null) {
                        return false;
                }
                LocalTime targetStart = target.getStartTime();
                LocalTime targetEnd = target.getEndTime();
                LocalTime otherStart = other.getStartTime();
                LocalTime otherEnd = other.getEndTime();
                return targetStart.isBefore(otherEnd) && targetEnd.isAfter(otherStart);
        }

        private DayWeek mapJavaDayOfWeekToCustom(java.time.DayOfWeek javaDayOfWeek) {
                if (javaDayOfWeek == null) {
                        throw new IllegalArgumentException("java.time.DayOfWeek não pode ser nulo para mapeamento.");
                }
                return DayWeek.valueOf(javaDayOfWeek.name()); // Assumindo que os enums têm nomes correspondentes
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
                                        disciplineNameStr, withWhom);
                        log.warn("Schedule/Participation failed: {} (ID: {}) has a time conflict. Conflicting tutoring details: {}",
                                        userRole, user.getId(), conflictDetails);
                        throw new TutoringOperationException(String.format(
                                        "O %s (você) já possui uma mentoria (%s) que conflita com este horário (%s das %s às %s). Detalhe do conflito: %s",
                                        userRole, disciplineNameStr,
                                        (date != null ? date.format(TutoringMapper.DATE_FORMATTER) : "N/A"),
                                        (startTime != null ? startTime.format(TutoringMapper.TIME_FORMATTER) : "N/A"),
                                        (endTime != null ? endTime.format(TutoringMapper.TIME_FORMATTER) : "N/A"),
                                        conflictDetails));
                }
                log.debug("No time conflicts found with existing tutorings for {} (ID: {})", userRole, user.getId());
        }
}