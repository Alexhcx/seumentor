package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringParticipantInfo;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRatingRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringRating;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRatingRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class TutoringQueryAdapter implements TutoringQuery {

    private final TutoringRepository tutoringRepository;
    private final TutoringRatingRepository tutoringRatingRepository;
    private static final Logger log = LoggerFactory.getLogger(TutoringQueryAdapter.class);

    @Override
    @Transactional(readOnly = true)
    public boolean existsTutoringForDiscipline(Long disciplineId) {
        return tutoringRepository.existsByDisciplineId(disciplineId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TutoringRepresentation> findTutoringById(Long tutoringId) {
        log.debug("Attempting to find Tutoring by ID: {}", tutoringId);
        return tutoringRepository.findById(tutoringId)
                .map(this::mapToRepresentation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findFilteredTutorings(Long mentorId, Long disciplineId, StatusTutoring status) {
        log.debug("Finding Tutorings with filters - MentorId: {}, DisciplineId: {}, Status: {}", mentorId, disciplineId, status);

        Specification<Tutoring> spec = Specification.where(null);

        if (mentorId != null) {
            spec = spec.and(TutoringSpecifications.withMentorId(mentorId));
        }
        if (disciplineId != null) {
            spec = spec.and(TutoringSpecifications.withDisciplineId(disciplineId));
        }
        if (status != null) {
            spec = spec.and(TutoringSpecifications.withStatus(status));
        }

        List<Tutoring> tutorings = tutoringRepository.findAll(spec);

        log.debug("Found {} tutorings matching criteria.", tutorings.size());

        return tutorings.stream()
                .map(this::mapToRepresentation)
                .collect(Collectors.toList());
    }

    static class TutoringSpecifications {

        public static Specification<Tutoring> withMentorId(Long mentorId) {
            return (root, query, criteriaBuilder) -> {
                return criteriaBuilder.equal(root.get("mentor").get("id"), mentorId);
            };
        }

        public static Specification<Tutoring> withDisciplineId(Long disciplineId) {
            return (root, query, criteriaBuilder) -> {
                return criteriaBuilder.equal(root.get("discipline").get("id"), disciplineId);
            };
        }

        public static Specification<Tutoring> withStatus(StatusTutoring status) {
            return (root, query, criteriaBuilder) -> {
                return criteriaBuilder.equal(root.get("status"), status);
            };
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRatingRepresentation> findAllTutoringRatings() {
        log.debug("Attempting to find all tutoring ratings.");
        List<TutoringRating> ratings = tutoringRatingRepository.findAll();
        log.info("Found {} tutoring ratings.", ratings.size());

        return ratings.stream()
                .map(rating -> {
                    Long raterUserId = null;
                    if (rating.getTutoring() != null) {
                        Tutoring tutoring = rating.getTutoring();
                        User mentor = tutoring.getMentor();
                        if (mentor != null && tutoring.getTopics() != null) {
                            Optional<TutoringParticipants> raterParticipant = tutoring.getTopics().stream()
                                    .filter(p -> p.getUser() != null && !p.getUser().getId().equals(mentor.getId()))
                                    .findFirst();
                            if (raterParticipant.isPresent()) {
                                raterUserId = raterParticipant.get().getUser().getId();
                            } else {
                                log.warn("Could not determine rater for rating ID {} (Tutoring ID {}). No non-mentor participant found.",
                                        rating.getId(), tutoring.getId());
                            }
                        } else {
                            log.warn("Could not determine rater for rating ID {}. Mentor or Topics are null for Tutoring ID {}.",
                                    rating.getId(), tutoring.getId());
                        }
                    } else {
                        log.warn("Could not determine rater for rating ID {}. Associated tutoring is null.", rating.getId());
                    }
                    return this.mapToRatingRepresentation(rating, raterUserId);
                })
                .collect(Collectors.toList());
    }

    private TutoringRepresentation mapToRepresentation(Tutoring tutoring) {
        if (tutoring == null) {
            return null;
        }

        // Lógica copiada do TutoringCommandService
        // ... (incluir a lógica completa do mapToRepresentation aqui) ...

        String mentorName = null;
        Long mentorId = null;
        if (tutoring.getMentor() != null) {
            try {
                mentorId = tutoring.getMentor().getId();
                mentorName = tutoring.getMentor().getFirstName() + " " + tutoring.getMentor().getLastName();
            } catch (EntityNotFoundException e) {
                log.warn("Mentor associated with tutoring {} not found during mapping.", tutoring.getId());
                mentorName = "[Mentor Inválido]";
            }
        }

        String disciplineName = null;
        Long disciplineId = null;
        if (tutoring.getDiscipline() != null) {
            try {
                disciplineId = tutoring.getDiscipline().getId();
                disciplineName = tutoring.getDiscipline().getDisciplineName();
            } catch (EntityNotFoundException e) {
                log.warn("Discipline associated with tutoring {} not found during mapping.", tutoring.getId());
                disciplineName = "[Disciplina Inválida]";
            }
        }

        Set<TutoringParticipantInfo> participantsInfo = new HashSet<>();
        if (tutoring.getTopics() != null) {
            participantsInfo = tutoring.getTopics().stream()
                    .map(p -> {
                        String participantName = "[Participante Inválido]";
                        Long participantId = null;
                        if (p.getUser() != null) {
                            try {
                                participantId = p.getUser().getId();
                                participantName = p.getUser().getFirstName() + " " + p.getUser().getLastName();
                            } catch (EntityNotFoundException e) {
                                log.warn("Participant user associated with tutoring {} not found during mapping.", tutoring.getId());
                            }
                        }
                        return new TutoringParticipantInfo(participantId, participantName, p.getTopic());
                    })
                    .collect(Collectors.toSet());
        }


        return new TutoringRepresentation(
                tutoring.getId(),
                mentorId,
                mentorName,
                disciplineId,
                disciplineName,
                tutoring.getClassType(),
                tutoring.getStatus(),
                tutoring.getStartTime(),
                tutoring.getEndTime(),
                tutoring.getLocal(),
                tutoring.getLinkVideo(),
                tutoring.getMaxParticipants(),
                tutoring.getIsChatEnable(),
                participantsInfo
        );
    }
    @Override
    public TutoringRatingRepresentation mapToRatingRepresentation(TutoringRating rating, Long raterUserId) {
        if (rating == null) {
            return null;
        }
        return new TutoringRatingRepresentation(
                rating.getId(),
                rating.getTutoring() != null ? rating.getTutoring().getId() : null,
                raterUserId,
                rating.getMentorRating(),
                rating.getReview(),
                rating.getCreatedAt(),
                rating.getUpdatedAt()
        );
    }
}