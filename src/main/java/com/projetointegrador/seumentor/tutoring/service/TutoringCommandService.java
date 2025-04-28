package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.course.api.DisciplineQuery;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.tutoring.api.TutoringCommand;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.tutoring.model.*;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRatingRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;
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

import java.time.LocalDateTime;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class TutoringCommandService implements TutoringCommand {

    private final TutoringRepository tutoringRepository;
    private final TutoringRatingRepository tutoringRatingRepository;
    private final TutoringParticipantsRepository tutoringParticipantsRepository;
    private final UserQuery userQuery;
    private final DisciplineQuery disciplineQuery;
    private final TutoringQuery tutoringQuery;

    private static final Logger log = LoggerFactory.getLogger(TutoringCommandService.class);

    @Override
    @Transactional
    public TutoringRepresentation scheduleTutoring(ScheduleTutoringRequest request) throws Exception {
        log.info("Attempting to schedule tutoring with request: {}", request);

        if (request.startTime().isAfter(request.endTime())) {
            throw new TutoringOperationException("Hora de início deve ser anterior à hora de fim.");
        }
        if (request.tutoringDate().isBefore(java.time.LocalDate.now())) {
            throw new TutoringOperationException("A data da monitoria não pode ser no passado.");
        }

        User mentor;
        User mentee;
        Discipline discipline;

        try {
            mentor = userQuery.getUserReferenceById(request.mentorId());
            log.debug("Mentor reference obtained for ID: {}", request.mentorId());

            mentee = userQuery.getUserReferenceById(request.menteeId());
            log.debug("Mentee reference obtained for ID: {}", request.menteeId());

            discipline = disciplineQuery.findBasicInfoById(request.disciplineId())
                    .map(info -> disciplineQuery.getReferenceById(info.id()))
                    .orElseThrow(() -> new DisciplineNotFoundException("Disciplina não encontrada com ID: " + request.disciplineId()));
            log.debug("Discipline reference obtained for ID: {}", request.disciplineId());

        } catch (EntityNotFoundException | DisciplineNotFoundException e) {
            log.warn("Failed to get references for scheduling: {}", e.getMessage());
            throw new TutoringOperationException("Falha ao obter dados necessários: " + e.getMessage());
        }

        Tutoring newTutoring = Tutoring.builder()
                .mentor(mentor)
                .discipline(discipline)
                .classType(request.classType())
                .status(StatusTutoring.AGENDADA)
                .tutoringDate(request.tutoringDate())
                .startTime(LocalDateTime.of(request.tutoringDate(), request.startTime()))
                .endTime(LocalDateTime.of(request.tutoringDate(), request.endTime()))
                .isChatEnable(false)
                .topics(new HashSet<>())
                .build();

        Tutoring savedTutoring = tutoringRepository.save(newTutoring);
        log.info("Tutoring created successfully with ID: {}", savedTutoring.getId());

        TutoringParticipants participant = TutoringParticipants.builder()
                .tutoring(savedTutoring)
                .user(mentee)
                .topic(request.topic())
                .build();

        tutoringParticipantsRepository.save(participant);
        log.info("Mentee {} added as participant to tutoring {} with topic '{}'", mentee.getId(), savedTutoring.getId(), request.topic());

        Tutoring tutoringWithParticipant = tutoringRepository.findById(savedTutoring.getId()).orElse(savedTutoring);

        return tutoringQuery.findTutoringById(savedTutoring.getId())
                .orElseThrow(() -> new IllegalStateException("Falha ao buscar monitoria recém-criada: " + savedTutoring.getId()));
    }

    @Override
    @Transactional
    public TutoringRepresentation confirmAndUpdatetutoring(Long tutoringId, ConfirmTutoringRequest request, Authentication authentication)
            throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException {

        log.info("Attempting to confirm and update tutoring ID: {} by user {}", tutoringId, authentication.getName());

        Tutoring tutoring = tutoringRepository.findById(tutoringId)
                .orElseThrow(() -> {
                    log.warn("Confirm tutoring failed: Tutoring not found with ID: {}", tutoringId);
                    return new TutoringNotFoundException("Monitoria não encontrada com ID: " + tutoringId);
                });

        if (tutoring.getStatus() != StatusTutoring.AGENDADA) {
            log.warn("Confirm tutoring failed: Tutoring ID {} is not in AGENDADA status (current: {})", tutoringId, tutoring.getStatus());
            throw new TutoringOperationException("A monitoria só pode ser confirmada se estiver no status AGENDADA.");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User mentor = tutoring.getMentor();
        if (mentor == null) {
            log.error("Confirm tutoring failed: Mentor reference is null for tutoring ID: {}", tutoringId);
            throw new IllegalStateException("Inconsistência de dados: Mentor não associado à monitoria.");
        }
        if (!userDetails.getUsername().equals(mentor.getEmail())) {
            log.warn("Confirm tutoring failed: User {} is not the mentor ({}) for tutoring ID {}",
                    userDetails.getUsername(), mentor.getEmail(), tutoringId);
            throw new AccessDeniedException("Usuário não autorizado a confirmar esta monitoria.");
        }

        if (tutoring.getClassType() == ClassType.ONLINE) {
            if (!StringUtils.hasText(request.linkVideo())) {
                log.warn("Confirm tutoring failed: linkVideo is required for ONLINE class type. Tutoring ID: {}", tutoringId);
                throw new TutoringOperationException("Link do vídeo é obrigatório para monitorias online.");
            }
            tutoring.setLocal(null);
        } else if (tutoring.getClassType() == ClassType.PRESENCIAL) {
            if (!StringUtils.hasText(request.local())) {
                log.warn("Confirm tutoring failed: local is required for PRESENCIAL class type. Tutoring ID: {}", tutoringId);
                throw new TutoringOperationException("Local é obrigatório para monitorias presenciais.");
            }
            tutoring.setLinkVideo(null);
        }

        tutoring.setLocal(request.local());
        tutoring.setLinkVideo(request.linkVideo());
        tutoring.setMaxParticipants(request.maxParticipants());
        tutoring.setIsChatEnable(request.isChatEnable());
        tutoring.setStatus(StatusTutoring.AGENDADA);

        Tutoring updatedTutoring = tutoringRepository.save(tutoring);
        log.info("Tutoring ID: {} confirmed and updated successfully by mentor {}", tutoringId, userDetails.getUsername());

        return tutoringQuery.findTutoringById(updatedTutoring.getId())
                .orElseThrow(() -> new IllegalStateException("Falha ao buscar monitoria recém-atualizada: " + updatedTutoring.getId()));
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
                    return new TutoringNotFoundException("Monitoria não encontrada com ID: " + tutoringId);
                });

        if (tutoring.getStatus() != StatusTutoring.PENDENTE) {
            log.warn("User {} attempted to delete tutoring ID {} with status {}, but only PENDENTE status allows deletion.",
                    requestingUser, tutoringId, tutoring.getStatus());
            throw new TutoringOperationException("Monitoria só pode ser excluída se estiver com status PENDENTE.");
        }

        tutoringRepository.deleteById(tutoringId);
        log.info("Tutoring ID: {} with status PENDENTE successfully deleted by user {}", tutoringId, requestingUser);
    }


    @Override
    @Transactional
    public TutoringRepresentation updateTutoringStatus(Long tutoringId, UpdateTutoringStatusRequest request, Authentication authentication)
            throws TutoringNotFoundException {

        String requestingUserEmail = (authentication != null) ? authentication.getName() : "UNKNOWN";
        log.info("Attempting simple status update for tutoring ID: {} to {} by user {}", tutoringId, request.status(), requestingUserEmail);

        Tutoring tutoring = tutoringRepository.findById(tutoringId)
                .orElseThrow(() -> {
                    log.warn("Simple update status failed: Tutoring not found with ID: {}", tutoringId);
                    return new TutoringNotFoundException("Monitoria não encontrada com ID: " + tutoringId);
                });

        StatusTutoring newStatus = request.status();
        StatusTutoring currentStatus = tutoring.getStatus();

        tutoring.setStatus(newStatus);

        Tutoring updatedTutoring = tutoringRepository.save(tutoring);
        log.info("Status for tutoring ID: {} updated from {} to {} by user {}", tutoringId, currentStatus, newStatus, requestingUserEmail);
        return tutoringQuery.findTutoringById(updatedTutoring.getId())
                .orElseThrow(() -> new IllegalStateException("Falha ao buscar monitoria recém-atualizada: " + updatedTutoring.getId()));
    }

    @Override
    @Transactional
    public TutoringRepresentation addParticipant(Long tutoringId, AddParticipantRequest request, Authentication authentication)
            throws TutoringNotFoundException, UserNotFoundException, TutoringOperationException {


        String requestingUserEmail = (authentication != null) ? authentication.getName() : "UNKNOWN";
        log.info("User {} attempting to add participant with ID {} to tutoring ID {}", requestingUserEmail, request.userId(), tutoringId);

        Tutoring tutoring = tutoringRepository.findById(tutoringId)
                .orElseThrow(() -> {
                    log.warn("Add participant failed: Tutoring not found with ID: {}", tutoringId);
                    return new TutoringNotFoundException("Monitoria não encontrada com ID: " + tutoringId);
                });

        if (tutoring.getStatus() != StatusTutoring.AGENDADA) { // Mantenha ou ajuste conforme sua regra
            log.warn("Add participant failed: Tutoring ID {} is not in AGENDADA status (current: {}).", tutoringId, tutoring.getStatus());
            throw new TutoringOperationException("Não é possível se inscrever em uma monitoria que não está agendada.");
        }

        Integer maxParticipants = tutoring.getMaxParticipants();
        int currentParticipants = tutoring.getTopics().size();

        if (maxParticipants != null && currentParticipants >= maxParticipants) {
            log.warn("Add participant failed: Tutoring ID {} is full (max: {}, current: {}).", tutoringId, maxParticipants, currentParticipants);
            throw new TutoringOperationException("Monitoria lotada. Não há mais vagas disponíveis.");
        }

        User participantUser;
        try {
            participantUser = userQuery.getUserReferenceById(request.userId());
        } catch (EntityNotFoundException e) {
            log.warn("Add participant failed: User with ID {} not found.", request.userId());
            throw new UserNotFoundException("Usuário não encontrado com ID: " + request.userId());
        }

        boolean alreadyParticipating = tutoring.getTopics().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(request.userId()));
        if (alreadyParticipating) {
            log.warn("Add participant failed: User ID {} is already participating in tutoring ID {}.", request.userId(), tutoringId);
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
                .orElseThrow(() -> new IllegalStateException("Falha ao buscar monitoria recém-atualizada após adicionar participante: " + tutoringId));
    }

    @Override
    @Transactional
    public TutoringRatingRepresentation addTutoringRating(Long tutoringId, TutoringRatingRequest request, Authentication authentication)
            throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException, UserNotFoundException {

        String requestingUserEmail = authentication.getName();
        log.info("User {} attempting to add rating for tutoring ID: {}", requestingUserEmail, tutoringId);

        Tutoring tutoring = tutoringRepository.findById(tutoringId)
                .orElseThrow(() -> {
                    log.warn("Add rating failed: Tutoring not found with ID: {}", tutoringId);
                    return new TutoringNotFoundException("Monitoria não encontrada com ID: " + tutoringId);
                });

        User rater = userQuery.findByEmail(requestingUserEmail)
                .map(userRep -> userQuery.getUserReferenceById(userRep.id())) // Get User entity reference
                .orElseThrow(() -> {
                    log.error("Add rating failed: Authenticated user {} not found in DB.", requestingUserEmail);
                    return new UserNotFoundException("Usuário autenticado não encontrado: " + requestingUserEmail);
                });

        boolean isParticipant = tutoring.getTopics().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(rater.getId()));

        if (!isParticipant) {
            log.warn("Add rating failed: User {} is not a participant of tutoring ID {}", requestingUserEmail, tutoringId);
            throw new AccessDeniedException("Usuário não autorizado a avaliar esta monitoria.");
        }

        if (tutoring.getStatus() != StatusTutoring.CONCLUIDA) {
            log.warn("Add rating failed: Tutoring ID {} is not CONCLUIDA (current: {})", tutoringId, tutoring.getStatus());
            throw new TutoringOperationException("Só é possível avaliar mentorias concluídas.");
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

        // 5. Update Tutoring link (Often managed by JPA/mappedBy, check your entity setup)
        // Se a relação Tutoring <-> TutoringRating precisar de atualização manual:
        // tutoring.setRating(savedRating);
        // tutoringRepository.save(tutoring);

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

}