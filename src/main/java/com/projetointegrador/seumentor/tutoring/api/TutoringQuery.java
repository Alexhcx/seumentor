package com.projetointegrador.seumentor.tutoring.api;

import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRatingRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringRating;

import java.util.List;
import java.util.Optional;

public interface TutoringQuery {
    boolean existsTutoringForDiscipline(Long disciplineId);
    Optional<TutoringRepresentation> findTutoringById(Long tutoringId);
    List<TutoringRepresentation> findFilteredTutorings(Long mentorId, Long disciplineId, StatusTutoring status);
    TutoringRatingRepresentation mapToRatingRepresentation(TutoringRating rating, Long raterUserId);
    List<TutoringRatingRepresentation> findAllTutoringRatings();
}
