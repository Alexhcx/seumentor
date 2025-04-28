package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação simplificada de uma disciplina, contendo apenas ID e nome")
public record SimpleDisciplineRepresentation(
        @Schema(description = "ID único da disciplina", example = "15")
        Long id,

        @Schema(description = "Nome da disciplina", example = "Cálculo I")
        String name
) {}
