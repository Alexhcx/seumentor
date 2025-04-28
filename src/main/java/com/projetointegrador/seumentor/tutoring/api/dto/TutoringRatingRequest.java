package com.projetointegrador.seumentor.tutoring.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record TutoringRatingRequest(
        @NotNull
        @Min(value = 1)
        @Max(value = 5)
        Float mentorRating,
        @Length(max = 1000)
        String review
) {}
