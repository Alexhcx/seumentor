package com.projetointegrador.seumentor.tutoring.api.dto;

import java.time.LocalDateTime;

public record TutoringRatingRepresentation(
        Long id,
        Long tutoringId,
        Long ratedByUserId,
        Float mentorRating,
        String review,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
