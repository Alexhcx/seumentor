package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@Schema(description = "Dados necessários para avaliar uma mentoria concluída")
public record TutoringRatingRequest(
        @NotNull
        @Min(value = 1, message = "A nota deve ser no mínimo 1")
        @Max(value = 5, message = "A nota deve ser no máximo 5")
        @Schema(description = "Nota dada pelo participante ao mentor (1 a 5)", example = "4.5", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Float mentorRating,

        @Length(max = 1000)
        @Schema(description = "Comentário opcional sobre a mentoria (máximo 1000 caracteres)", example = "O mentor foi muito claro e paciente, ajudou bastante!")
        String review
) {}
