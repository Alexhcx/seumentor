// Arquivo: src/main/java/com/projetointegrador/seumentor/tutoring/api/TutoringQuery.java
package com.projetointegrador.seumentor.tutoring.api;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.tutoring.api.dto.MentorProfileRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRatingRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.UserAvailabilityRepresentation;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.MentorAvailability; // Import necessário
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

    // Mantendo a correção da interação anterior para este método,
    // que alinha o nome e o tipo de retorno com a implementação que você já tinha.
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

    // ----- MÉTODOS ADICIONADOS À INTERFACE -----
    @Transactional(readOnly = true)
    Optional<UserAvailabilityRepresentation> findMentorAvailabilityRepresentationById(Long availabilityId);

    @Transactional(readOnly = true)
    List<UserAvailabilityRepresentation> findMentorAvailabilitiesByMentorId(Long mentorId);

    @Transactional(readOnly = true)
    Optional<MentorProfileRepresentation> findMentorProfileByMentorId(Long mentorId);

    @Transactional(readOnly = true)
    List<MentorProfileRepresentation> findAllMentorProfiles();

    @Transactional(readOnly = true)
    List<MentorAvailability> findAllAvailabilitiesByDayOfWeek(DayWeek dayOfWeek);
}