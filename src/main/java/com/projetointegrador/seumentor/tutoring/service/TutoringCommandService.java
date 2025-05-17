package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.course.api.DisciplineQuery;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.tutoring.api.TutoringCommand;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.tutoring.model.*;
import com.projetointegrador.seumentor.tutoring.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRatingRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.enums.Role;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TutoringCommandService implements TutoringCommand {

        private final TutoringRepository tutoringRepository;
        private final TutoringRatingRepository tutoringRatingRepository;
        private final TutoringParticipantsRepository tutoringParticipantsRepository;
        private final UserQuery userQuery;
        private final DisciplineQuery disciplineQuery;
        private final TutoringQuery tutoringQuery;
        private final MentorAvailabilityRepository mentorAvailabilityRepository;

        private static final Logger log = LoggerFactory.getLogger(TutoringCommandService.class);
        private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        @Override
        @Transactional
        public ScheduledTutoringRepresentation scheduleTutoring(ScheduleTutoringRequest request) throws Exception {
                log.info("Attempting to schedule tutoring with request: {}", request);

                // 1. Validações básicas
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

                // 2. Obter referências
                User mentor;
                User mentee;
                Discipline discipline;
                try {
                        mentor = userQuery.getUserReferenceById(request.mentorId());
                        mentee = userQuery.getUserReferenceById(request.menteeId());
                        discipline = disciplineQuery.getReferenceById(request.disciplineId());
                } catch (EntityNotFoundException e) {
                        log.warn("Schedule failed: Could not find User or Discipline. Request: {}, Error: {}", request,
                                        e.getMessage());
                        throw new TutoringOperationException(
                                        "Falha ao obter dados necessários (usuário ou disciplina): " + e.getMessage());
                }

                // Status a serem considerados para conflito de MENTORIAS existentes
                List<StatusTutoring> statusesToCheckForConflict = Arrays.asList(
                                StatusTutoring.PENDENTE, StatusTutoring.AGENDADA, StatusTutoring.EM_ANDAMENTO);

                // 3. Verificar Duplicidade de Mentoria Específica (evitar agendar a mesma
                // coisa)
                boolean alreadyExists = tutoringRepository
                                .existsByMentorAndDisciplineAndTutoringDateAndStartTimeAndEndTimeAndStatusNotIn(
                                                mentor,
                                                discipline,
                                                request.tutoringDate(),
                                                request.startTime(),
                                                request.endTime(),
                                                Arrays.asList(StatusTutoring.CANCELADA, StatusTutoring.CONCLUIDA) // Não
                                                                                                                  // considera
                                                                                                                  // canceladas/concluídas
                                                                                                                  // como
                                                                                                                  // duplicatas
                                                                                                                  // ativas
                                );
                if (alreadyExists) {
                        log.warn("Schedule failed: Exact tutoring already exists and is active/pending. Request: {}",
                                        request);
                        throw new TutoringOperationException(
                                        "Esta mentoria (mesmo mentor, disciplina, data e horário) já existe e está pendente ou agendada.");
                }

                // 4. Verificar Conflitos de Horário para o MENTEE (com outras mentorias
                // ativas/pendentes)
                checkUserConflictsWithExistingTutorings(mentee, request.tutoringDate(), request.startTime(),
                                request.endTime(),
                                statusesToCheckForConflict, "mentorado");

                // 5. Verificar Conflitos de Horário para o MENTOR com OUTRAS MENTORIAS
                // ativas/pendentes
                checkUserConflictsWithExistingTutorings(mentor, request.tutoringDate(), request.startTime(),
                                request.endTime(),
                                statusesToCheckForConflict, "mentor");

                // 6. Validação CORRIGIDA: Verificar se a mentoria solicitada está DENTRO de uma
                // disponibilidade VÁLIDA e ATIVA do mentor
                DayWeek dayOfWeek = mapJavaDayOfWeekToCustom(request.tutoringDate().getDayOfWeek());
                Optional<MentorAvailability> coveringAvailability = mentorAvailabilityRepository
                                .findCoveringAndActiveAvailability(
                                                mentor,
                                                discipline, // Validar para a disciplina específica
                                                dayOfWeek,
                                                request.startTime(),
                                                request.endTime());

                if (coveringAvailability.isEmpty()) {
                        log.warn(
                                        "Schedule failed: Mentor (ID: {}) does not have an active and covering availability for discipline '{}' on {} from {} to {}.",
                                        mentor.getId(), discipline.getDisciplineName(), dayOfWeek,
                                        request.startTime().format(TIME_FORMATTER),
                                        request.endTime().format(TIME_FORMATTER));
                        throw new TutoringOperationException(String.format(
                                        "O mentor não possui disponibilidade ativa para a disciplina '%s' que cubra o horário solicitado (%s das %s às %s).",
                                        discipline.getDisciplineName(),
                                        dayOfWeek.toString().toLowerCase().replace("_", " "),
                                        request.startTime().format(TIME_FORMATTER),
                                        request.endTime().format(TIME_FORMATTER)));
                }
                log.info("Found covering and active availability (ID: {}) for mentor {} for the requested tutoring.",
                                coveringAvailability.get().getId(), mentor.getId());

                // 7. Se todas as validações passaram, criar a mentoria
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
                savedTutoring.getTopics().add(firstParticipant);
                log.info("Mentee {} added as the first participant to tutoring {} with topic '{}'", mentee.getId(),
                                savedTutoring.getId(), request.topic());

                return mapToScheduledTutoringRepresentation(savedTutoring);
        }

        private void checkUserConflictsWithExistingTutorings(User user, LocalDate date, LocalTime startTime,
                        LocalTime endTime,
                        List<StatusTutoring> statusesToLookForConflictIn, String userRole)
                        throws TutoringOperationException {
                log.debug(
                                "Checking time conflicts for {} (ID: {}) on {} between {} and {} against existing tutorings with statuses: {}",
                                userRole, user.getId(), date, startTime, endTime, statusesToLookForConflictIn);

                List<Tutoring> conflictingTutorings = tutoringRepository
                                .findConflictingTutoringsForUserWithSpecificStatuses(
                                                user, date, startTime, endTime, statusesToLookForConflictIn);

                if (!conflictingTutorings.isEmpty()) {
                        Tutoring conflict = conflictingTutorings.get(0);
                        String conflictMenteeName = conflict.getTopics().stream()
                                        .findFirst()
                                        .map(tp -> tp.getUser().getFirstName() + " " + tp.getUser().getLastName())
                                        .orElse("um participante");

                        String withWhom = conflict.getMentor().getId().equals(user.getId())
                                        ? "com " + conflictMenteeName
                                        : "com o mentor " + conflict.getMentor().getFirstName() + " "
                                                        + conflict.getMentor().getLastName();

                        String conflictDetails = String.format("Mentoria ID %d (%s - %s) de '%s' %s.",
                                        conflict.getId(),
                                        conflict.getStartTime().format(TIME_FORMATTER),
                                        conflict.getEndTime().format(TIME_FORMATTER),
                                        conflict.getDiscipline().getDisciplineName(),
                                        withWhom);

                        log.warn("Schedule failed: {} (ID: {}) has a time conflict. Conflicting tutoring details: {}",
                                        userRole,
                                        user.getId(), conflictDetails);
                        throw new TutoringOperationException(String.format(
                                        "O %s (você) já possui uma mentoria (%s) que conflita com este horário (%s das %s às %s). Detalhe do conflito: %s",
                                        userRole,
                                        conflict.getDiscipline().getDisciplineName(), // Disciplina da mentoria
                                                                                      // conflitante
                                        date.format(DATE_FORMATTER),
                                        startTime.format(TIME_FORMATTER),
                                        endTime.format(TIME_FORMATTER),
                                        conflictDetails));
                }
                log.debug("No time conflicts found with existing tutorings for {} (ID: {})", userRole, user.getId());
        }

        private ScheduledTutoringRepresentation mapToScheduledTutoringRepresentation(Tutoring tutoring) {
                if (tutoring == null) {
                        return null;
                }

                String mentorName = "[Mentor Inválido]";
                Long mentorId = null;
                if (tutoring.getMentor() != null) {
                        try {
                                mentorId = tutoring.getMentor().getId();
                                mentorName = tutoring.getMentor().getFirstName() + " "
                                                + tutoring.getMentor().getLastName();
                        } catch (EntityNotFoundException e) {
                                log.warn("Mentor associated with tutoring {} not found during mapping for schedule response.",
                                                tutoring.getId());
                        }
                }

                String disciplineName = "[Disciplina Inválida]";
                Long disciplineId = null;
                if (tutoring.getDiscipline() != null) {
                        try {
                                disciplineId = tutoring.getDiscipline().getId();
                                disciplineName = tutoring.getDiscipline().getDisciplineName();
                        } catch (EntityNotFoundException e) {
                                log.warn("Discipline associated with tutoring {} not found during mapping for schedule response.",
                                                tutoring.getId());
                        }
                }

                List<String> topicsList = new ArrayList<>();
                int numberOfParticipants = 0;
                if (tutoring.getTopics() != null && !tutoring.getTopics().isEmpty()) {
                        numberOfParticipants = tutoring.getTopics().size();
                        topicsList = tutoring.getTopics().stream()
                                        .map(TutoringParticipants::getTopic)
                                        .collect(Collectors.toList());
                }

                String localValue;
                String linkVideoValue;

                if (tutoring.getTutoringClassType() == TutoringClassType.PRESENCIAL) {
                        localValue = "A definir"; // Ou tutoring.getLocal() se já puder ser definido no agendamento
                                                  // inicial
                        linkVideoValue = "Não se aplica";
                } else { // ONLINE
                        localValue = "Não se aplica";
                        linkVideoValue = tutoring.getLinkVideo() != null ? tutoring.getLinkVideo() : "Não se aplica";
                }

                // Se a mentoria já tiver um local/link definido (ex: veio de uma
                // disponibilidade que já tinha isso), usar esse valor.
                // A lógica acima é para o cenário PENDENTE inicial.
                // Para uma mentoria que já tem status AGENDADA e foi confirmada, os valores
                // reais de local/linkVideo devem ser usados.
                // No momento do schedule inicial, o status é PENDENTE, então "A definir" / "Não
                // se aplica" é apropriado.

                return new ScheduledTutoringRepresentation(
                                tutoring.getId(),
                                mentorId,
                                mentorName,
                                disciplineId,
                                disciplineName,
                                tutoring.getTutoringClassType(),
                                tutoring.getStatus(),
                                tutoring.getStartTime() != null ? tutoring.getStartTime().format(TIME_FORMATTER) : null,
                                tutoring.getEndTime() != null ? tutoring.getEndTime().format(TIME_FORMATTER) : null,
                                tutoring.getTutoringDate() != null ? tutoring.getTutoringDate().format(DATE_FORMATTER)
                                                : null,
                                localValue,
                                linkVideoValue,
                                tutoring.getMaxParticipants(),
                                numberOfParticipants,
                                tutoring.getIsChatEnable(),
                                topicsList);
        }

        @Override
        @Transactional
        public TutoringRepresentation confirmAndUpdatetutoring(Long tutoringId, ConfirmTutoringRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException {

                log.info("Attempting to confirm or update details for tutoring ID: {} by user {}", tutoringId,
                                authentication.getName());

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
                        log.warn(
                                        "Confirm/Update tutoring failed: Tutoring ID {} is not in PENDENTE or AGENDADA status (current: {})",
                                        tutoringId, tutoring.getStatus());
                        throw new TutoringOperationException(
                                        "A monitoria só pode ser confirmada/atualizada se estiver no status PENDENTE ou AGENDADA. Status atual: "
                                                        + tutoring.getStatus());
                }

                // Aplicar os detalhes da requisição
                if (tutoring.getTutoringClassType() == TutoringClassType.ONLINE) {
                        if (!StringUtils.hasText(request.linkVideo())) {
                                log.warn("Confirm/Update tutoring failed: linkVideo is required for ONLINE class type. Tutoring ID: {}",
                                                tutoringId);
                                throw new TutoringOperationException(
                                                "Link do vídeo é obrigatório para monitorias online.");
                        }
                        tutoring.setLocal("Não se aplica");
                        tutoring.setLinkVideo(request.linkVideo());
                } else if (tutoring.getTutoringClassType() == TutoringClassType.PRESENCIAL) {
                        if (!StringUtils.hasText(request.local())) {
                                log.warn("Confirm/Update tutoring failed: local is required for PRESENCIAL class type. Tutoring ID: {}",
                                                tutoringId);
                                throw new TutoringOperationException(
                                                "Local é obrigatório para monitorias presenciais.");
                        }
                        tutoring.setLinkVideo("Não se aplica");
                        tutoring.setLocal(request.local());
                }

                tutoring.setMaxParticipants(request.maxParticipants());
                tutoring.setIsChatEnable(request.isChatEnable());

                // Se estava PENDENTE, agora se torna AGENDADA.
                // Se já estava AGENDADA, o status permanece AGENDADA.
                if (tutoring.getStatus() == StatusTutoring.PENDENTE) {
                        tutoring.setStatus(StatusTutoring.AGENDADA);
                        log.info("Tutoring ID: {} confirmed by mentor {}. Status changed from PENDENTE to AGENDADA.",
                                        tutoringId,
                                        userDetails.getUsername());
                } else {
                        log.info("Tutoring ID: {} (status AGENDADA) details updated by mentor {}.", tutoringId,
                                        userDetails.getUsername());
                }

                Tutoring updatedTutoring = tutoringRepository.save(tutoring);

                if (this.tutoringQuery == null) {
                        log.error("TutoringQuery not injected in TutoringCommandService for confirmAndUpdatetutoring");
                        throw new IllegalStateException("TutoringQuery service not available for mapping.");
                }
                return this.tutoringQuery.findTutoringById(updatedTutoring.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "Falha ao buscar monitoria recém-atualizada: "
                                                                + updatedTutoring.getId()));
        }

        @Override
        @Transactional
        public void deleteOrCancelTutoring(Long tutoringId, Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException {

                String requestingUser = (authentication != null) ? authentication.getName() : "UNKNOWN";
                log.info("Attempting to delete tutoring ID: {} requested by user {}", tutoringId, requestingUser);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Delete tutoring failed: Tutoring not found with ID: {}", tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                if (tutoring.getStatus() != StatusTutoring.PENDENTE) {
                        log.warn(
                                        "User {} attempted to delete tutoring ID {} with status {}, but only PENDENTE status allows deletion.",
                                        requestingUser, tutoringId, tutoring.getStatus());
                        throw new TutoringOperationException(
                                        "Monitoria só pode ser excluída se estiver com status PENDENTE.");
                }

                tutoringRepository.deleteById(tutoringId);
                log.info("Tutoring ID: {} with status PENDENTE successfully deleted by user {}", tutoringId,
                                requestingUser);
        }

        @Override
        @Transactional
        public TutoringRepresentation updateTutoringStatus(Long tutoringId, UpdateTutoringStatusRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException {

                String requestingUserEmail = (authentication != null) ? authentication.getName() : "UNKNOWN";
                log.info("Attempting simple status update for tutoring ID: {} to {} by user {}", tutoringId,
                                request.status(),
                                requestingUserEmail);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Simple update status failed: Tutoring not found with ID: {}",
                                                        tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                StatusTutoring newStatus = request.status();
                StatusTutoring currentStatus = tutoring.getStatus();

                tutoring.setStatus(newStatus);

                Tutoring updatedTutoring = tutoringRepository.save(tutoring);
                log.info("Status for tutoring ID: {} updated from {} to {} by user {}", tutoringId, currentStatus,
                                newStatus,
                                requestingUserEmail);
                return tutoringQuery.findTutoringById(updatedTutoring.getId())
                                .orElseThrow(() -> new IllegalStateException(
                                                "Falha ao buscar monitoria recém-atualizada: "
                                                                + updatedTutoring.getId()));
        }

        @Override
        @Transactional
        public TutoringRepresentation addParticipant(Long tutoringId, AddParticipantRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, UserNotFoundException, TutoringOperationException {

                String requestingUserEmail = (authentication != null) ? authentication.getName() : "UNKNOWN";
                log.info("User {} attempting to add participant with ID {} to tutoring ID {}", requestingUserEmail,
                                request.userId(), tutoringId);

                Tutoring tutoring = tutoringRepository.findById(tutoringId)
                                .orElseThrow(() -> {
                                        log.warn("Add participant failed: Tutoring not found with ID: {}", tutoringId);
                                        return new TutoringNotFoundException(
                                                        "Monitoria não encontrada com ID: " + tutoringId);
                                });

                if (tutoring.getStatus() != StatusTutoring.AGENDADA) {
                        log.warn("Add participant failed: Tutoring ID {} is not in AGENDADA status (current: {}).",
                                        tutoringId,
                                        tutoring.getStatus());
                        throw new TutoringOperationException(
                                        "Não é possível se inscrever em uma monitoria que não está agendada.");
                }

                User participantUser;
                try {
                        participantUser = userQuery.getUserReferenceById(request.userId());
                } catch (EntityNotFoundException e) {
                        log.warn("Add participant failed: User with ID {} not found.", request.userId());
                        throw new UserNotFoundException("Usuário não encontrado com ID: " + request.userId());
                }

                checkUserTutoringConflicts(
                                participantUser,
                                tutoring.getMentor(),
                                tutoring.getTutoringDate(),
                                tutoring.getStartTime(),
                                tutoring.getEndTime(),
                                this.userQuery,
                                log);

                Integer maxParticipants = tutoring.getMaxParticipants();
                int currentParticipants = tutoring.getTopics().size();

                if (maxParticipants != null && currentParticipants >= maxParticipants) {
                        log.warn("Add participant failed: Tutoring ID {} is full (max: {}, current: {}).", tutoringId,
                                        maxParticipants, currentParticipants);
                        throw new TutoringOperationException("Monitoria lotada. Não há mais vagas disponíveis.");
                }

                boolean alreadyParticipating = tutoring.getTopics().stream()
                                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(request.userId()));
                if (alreadyParticipating) {
                        log.warn("Add participant failed: User ID {} is already participating in tutoring ID {}.",
                                        request.userId(),
                                        tutoringId);
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
                                .map(userRep -> userQuery.getUserReferenceById(userRep.id())) // Get User entity
                                                                                              // reference
                                .orElseThrow(() -> {
                                        log.error("Add rating failed: Authenticated user {} not found in DB.",
                                                        requestingUserEmail);
                                        return new UserNotFoundException(
                                                        "Usuário autenticado não encontrado: " + requestingUserEmail);
                                });

                boolean isParticipant = tutoring.getTopics().stream()
                                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(rater.getId()));

                if (!isParticipant) {
                        log.warn("Add rating failed: User {} is not a participant of tutoring ID {}",
                                        requestingUserEmail,
                                        tutoringId);
                        throw new AccessDeniedException("Usuário não autorizado a avaliar esta monitoria.");
                }

                if (tutoring.getRating() != null) {
                        log.warn("Add rating failed: Tutoring ID {} already has a rating.", tutoringId);
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
        public void deleteTutoringRating(Long ratingId) throws TutoringOperationException {
                log.info("Attempting to delete tutoring rating with ID: {}", ratingId);

                if (!tutoringRatingRepository.existsById(ratingId)) {
                        log.warn("Delete rating failed: Rating not found with ID: {}", ratingId);
                        throw new TutoringOperationException("Avaliação não encontrada com ID: " + ratingId);
                }

                tutoringRatingRepository.deleteById(ratingId);

                log.info("Tutoring rating with ID: {} deleted successfully.", ratingId);
        }

        private static DayWeek mapJavaDayOfWeekToCustom(java.time.DayOfWeek javaDayOfWeek) {
                if (javaDayOfWeek == null) {
                        throw new IllegalArgumentException("java.time.DayOfWeek não pode ser nulo para mapeamento.");
                }
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

        private static void checkUserTutoringConflicts(
                        User userToCheck,
                        User actualMentorForTutoring,
                        LocalDate tutoringDate,
                        LocalTime tutoringStartTime,
                        LocalTime tutoringEndTime,
                        UserQuery userQueryInstance,
                        Logger logger) throws TutoringOperationException {

                if (actualMentorForTutoring != null && actualMentorForTutoring.getId().equals(userToCheck.getId())) {
                        logger.warn("Conflict Check Failed: User ID {} is attempting to be both mentor and mentee/participant.",
                                        userToCheck.getId());
                        throw new TutoringOperationException(
                                        "Um usuário não pode ser mentor e mentorado/participante na mesma mentoria.");
                }

                if (userToCheck.getRole() == Role.MENTOR) {
                        List<UserAvailabilityRepresentation> userAvailabilities = userQueryInstance
                                        .findAvailabilitiesRepresentationByUserId(userToCheck.getId());

                        if (tutoringDate != null && tutoringStartTime != null && tutoringEndTime != null) {
                                DayWeek tutoringDayOfWeekEnum = mapJavaDayOfWeekToCustom(tutoringDate.getDayOfWeek());

                                for (UserAvailabilityRepresentation availabilityRep : userAvailabilities) {
                                        if (Boolean.TRUE.equals(availabilityRep.isAvailable())
                                                        && availabilityRep.dayOfWeek() == tutoringDayOfWeekEnum) {
                                                // Verifica sobreposição de horários
                                                boolean overlaps = tutoringStartTime.isBefore(availabilityRep.endTime())
                                                                &&
                                                                tutoringEndTime.isAfter(availabilityRep.startTime());
                                                if (overlaps) {
                                                        logger.warn(
                                                                        "Conflict Check Failed: User ID {} (Role: MENTOR) has a conflicting availability (ID: {}) with the proposed tutoring time.",
                                                                        userToCheck.getId(), availabilityRep.id());
                                                        throw new TutoringOperationException(
                                                                        "Conflito de horário: Você possui uma disponibilidade como mentor que coincide com este horário de mentoria.");
                                                }
                                        }
                                }
                        }
                }
        }

        @Override
        @Transactional
        public void removeParticipant(Long tutoringId, Long userId, Authentication authentication)
                        throws TutoringNotFoundException, UserNotFoundException, TutoringOperationException,
                        AccessDeniedException {

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

                User userToRemove = userQuery.getUserReferenceById(userId);

                User authenticatedUser = userQuery.findByEmail(requestingUserEmail)
                                .map(rep -> userQuery.getUserReferenceById(rep.id()))
                                .orElseThrow(() -> new UserNotFoundException("Usuário autenticado não encontrado."));

                if (!authenticatedUser.getId().equals(userId) && !authenticatedUser.getRole().equals(Role.ADMIN)) {
                        log.warn("Service: Usuário {} não autorizado a remover participante {} da mentoria {}",
                                        requestingUserEmail, userId, tutoringId);
                        throw new AccessDeniedException("Você não tem permissão para remover este participante.");
                }

                if (tutoring.getMentor() != null && tutoring.getMentor().getId().equals(userId)) {
                        log.warn("Service: Tentativa de remover o mentor (ID: {}) como participante da mentoria ID: {}",
                                        userId, tutoringId);
                        throw new TutoringOperationException(
                                        "O mentor não pode sair da mentoria como participante. Para cancelar a mentoria, utilize a opção apropriada.");
                }

                TutoringParticipants participantToRemove = tutoring.getTopics().stream()
                                .filter(p -> p.getUser() != null && p.getUser().getId().equals(userId))
                                .findFirst()
                                .orElseThrow(() -> {
                                        log.warn("Service: Usuário ID {} não é um participante da mentoria ID {}",
                                                        userId, tutoringId);
                                        return new TutoringOperationException(
                                                        "Usuário não é participante desta monitoria.");
                                });

                if (tutoring.getStatus() == StatusTutoring.PENDENTE
                                || tutoring.getStatus() == StatusTutoring.AGENDADA) {
                        tutoringParticipantsRepository.delete(participantToRemove);
                        tutoring.getTopics().remove(participantToRemove);

                        if (tutoring.getStatus() == StatusTutoring.PENDENTE && tutoring.getTopics().isEmpty()) {
                                log.info("Service: Mentoria PENDENTE ID {} ficou sem participantes após saída do usuário ID {}. Excluindo mentoria.",
                                                tutoringId, userId);
                                tutoringRepository.delete(tutoring);
                        } else {
                                tutoringRepository.save(tutoring);
                        }

                        log.info("Service: Participante ID {} removido com sucesso da mentoria ID {}", userId,
                                        tutoringId);
                } else {
                        log.warn("Service: Tentativa de sair da mentoria ID {} com status inválido: {}", tutoringId,
                                        tutoring.getStatus());
                        throw new TutoringOperationException(
                                        "Não é possível sair de uma mentoria que não esteja PENDENTE ou AGENDADA.");
                }
        }

        @Override
        @Transactional
        public TutoringRepresentation cancelMentorTutoring(Long userIdFromPath, Long tutoringId,
                        boolean deactivateAvailability, Authentication authentication) // userIdFromPath ADICIONADO
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException,
                        UserNotFoundException {

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
                                .map(rep -> userQuery.getUserReferenceById(rep.id())) // Assume que
                                                                                      // userQuery.getUserReferenceById(id)
                                                                                      // retorna User
                                .orElseThrow(() -> {
                                        log.error("Service: Cancel tutoring failed. Authenticated user {} not found.",
                                                        authenticatedUserEmail);
                                        return new UserNotFoundException("Usuário autenticado não encontrado: "
                                                        + authenticatedUserEmail);
                                });

                User userFromPath = userQuery.findById(userIdFromPath) // Busca o usuário pelo ID do caminho
                                .map(rep -> userQuery.getUserReferenceById(rep.id()))
                                .orElseThrow(() -> {
                                        log.warn("Service: Cancel tutoring failed. User specified in path (ID: {}) not found.",
                                                        userIdFromPath);
                                        return new UserNotFoundException(
                                                        "Usuário (mentor) especificado no caminho não encontrado com ID: "
                                                                        + userIdFromPath);
                                });

                // Lógica de Autorização Refinada
                boolean isAdmin = authenticatedUser.getAuthorities().stream()
                                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ADMIN"));

                if (isAdmin) {
                        // ADMIN pode cancelar, mas o userIdFromPath DEVE ser o mentor da mentoria.
                        if (!tutoring.getMentor().getId().equals(userFromPath.getId())) {
                                log.warn("Service (Admin Action): User ID in path {} is not the mentor of tutoring ID {}. Actual mentor is {}.",
                                                userFromPath.getId(), tutoringId, tutoring.getMentor().getId());
                                throw new AccessDeniedException("O usuário (ID: " + userFromPath.getId()
                                                + ") especificado no caminho não é o mentor desta monitoria (ID: "
                                                + tutoringId + ").");
                        }
                        log.info("Service: Admin {} is performing cancellation for mentor {}'s tutoring {}.",
                                        authenticatedUserEmail, userFromPath.getEmail(), tutoringId);
                } else {
                        // Se NÃO for ADMIN:
                        // 1. O usuário autenticado DEVE ser o mesmo que o userIdFromPath.
                        if (!authenticatedUser.getId().equals(userFromPath.getId())) {
                                log.warn("Service: Authenticated user {} (ID: {}) is not authorized to cancel tutoring for user ID in path {} (not an Admin).",
                                                authenticatedUserEmail, authenticatedUser.getId(),
                                                userFromPath.getId());
                                throw new AccessDeniedException(
                                                "Você não tem permissão para realizar esta ação para o usuário especificado (ID: "
                                                                + userFromPath.getId() + ").");
                        }
                        // 2. Esse usuário (que é o autenticado e o do path) DEVE ser o mentor da
                        // mentoria.
                        if (!tutoring.getMentor().getId().equals(authenticatedUser.getId())) { // authenticatedUser.getId()
                                                                                               // é igual a
                                                                                               // userFromPath.getId()
                                                                                               // aqui
                                log.warn("Service: Authenticated user {} (ID: {}) is not the mentor of tutoring ID {}. Actual mentor is {}.",
                                                authenticatedUserEmail, authenticatedUser.getId(), tutoringId,
                                                tutoring.getMentor().getId());
                                throw new AccessDeniedException(
                                                "Você não é o mentor desta monitoria (ID: " + tutoringId + ").");
                        }
                        log.info("Service: Mentor {} is performing cancellation for their tutoring {}.",
                                        authenticatedUserEmail, tutoringId);
                }

                // Verificação de Status (permanece a mesma)
                if (tutoring.getStatus() == StatusTutoring.CONCLUIDA
                                || tutoring.getStatus() == StatusTutoring.CANCELADA) {
                        log.warn("Service: Tutoring ID {} cannot be cancelled. Status is already {} or {}.",
                                        tutoringId, StatusTutoring.CONCLUIDA, StatusTutoring.CANCELADA);
                        throw new TutoringOperationException(
                                        "Monitoria não pode ser cancelada pois já está "
                                                        + tutoring.getStatus().toString().toLowerCase() + ".");
                }

                tutoring.setStatus(StatusTutoring.CANCELADA);
                Tutoring cancelledTutoring = tutoringRepository.save(tutoring);
                log.info("Service: Tutoring ID {} status changed to CANCELADA by authenticated user {} (acting for/as mentor {}).",
                                tutoringId, authenticatedUserEmail, userFromPath.getEmail());

                // Lógica para Desativar Disponibilidade (permanece a mesma, usa
                // tutoring.getMentor() que foi validado)
                if (deactivateAvailability) {
                        log.info("Service: Attempting to deactivate corresponding availability for tutoring ID {}.",
                                        tutoringId);
                        try {
                                User mentorOfTutoring = tutoring.getMentor(); // Este é o userFromPath validado
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
                                                availabilityToDeactivate.setIsAvailable(false);
                                                mentorAvailabilityRepository.save(availabilityToDeactivate);
                                                log.info("Service: Availability ID {} for mentor {} successfully deactivated due to tutoring {} cancellation.",
                                                                availabilityToDeactivate.getId(),
                                                                mentorOfTutoring.getId(), tutoringId);
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

}