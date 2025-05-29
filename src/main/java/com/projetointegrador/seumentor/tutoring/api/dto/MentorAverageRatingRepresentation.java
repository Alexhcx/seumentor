package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação da média de avaliações de um mentor e o total de avaliações")
public record MentorAverageRatingRepresentation(
        @Schema(description = "ID do mentor", example = "10")
        Long mentorId,

        @Schema(description = "Média das avaliações recebidas pelo mentor. Pode ser nulo se não houver avaliações.", example = "4.75")
        Double averageRating,

        @Schema(description = "Número total de avaliações consideradas para a média.", example = "20")
        Integer numberOfRatings
) {}
