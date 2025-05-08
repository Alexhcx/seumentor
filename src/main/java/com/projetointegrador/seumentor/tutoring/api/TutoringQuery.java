package com.projetointegrador.seumentor.tutoring.api;

import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRatingRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringRating;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TutoringQuery {
    boolean existsTutoringForDiscipline(Long disciplineId);

    Optional<TutoringRepresentation> findTutoringById(Long tutoringId);

    List<TutoringRepresentation> findFilteredTutorings(Long mentorId, Long disciplineId, StatusTutoring status);

    @Transactional(readOnly = true)
    List<TutoringRepresentation> findAllTutoringsByMentorId(Long mentorId);

    @Transactional(readOnly = true)
    List<TutoringRepresentation> findAllTutoringsByParticipantId(Long userId);

    TutoringRatingRepresentation mapToRatingRepresentation(TutoringRating rating, Long raterUserId);

    List<TutoringRatingRepresentation> findAllTutoringRatings();

    @Transactional(readOnly = true)
    List<TutoringRepresentation> findTutoringAndAvailabilityByDate(LocalDate date);

    @Transactional(readOnly = true)
    List<TutoringRepresentation> findAvailableSlotsForUser(
            LocalDate date,
            Optional<Long> disciplineId,
            Long requestingUserId
    );
}
