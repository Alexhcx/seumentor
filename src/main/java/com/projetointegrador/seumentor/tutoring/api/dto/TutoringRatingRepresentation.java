package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Representação dos dados de uma avaliação de mentoria")
public record TutoringRatingRepresentation(
        @Schema(description = "ID único da avaliação", example = "301")
        Long id,

        @Schema(description = "ID da mentoria que foi avaliada", example = "150")
        Long tutoringId,

        @Schema(description = "ID do usuário que realizou a avaliação", example = "52")
        Long ratedByUserId,

        @Schema(description = "Nota dada ao mentor", example = "5.0")
        Float mentorRating,

        @Schema(description = "Comentário da avaliação", example = "Excelente mentoria!")
        String review,

        @Schema(description = "Data e hora de criação da avaliação", example = "2024-05-20T16:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Data e hora da última atualização da avaliação", example = "2024-05-20T16:05:00")
        LocalDateTime updatedAt
) {}
