package com.projetointegrador.seumentor.course.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação simplificada de uma Disciplina, contendo apenas ID e nome. Usado em contextos onde detalhes completos não são necessários.")
public record SimpleDisciplineRepresentation(
        @Schema(description = "ID único da disciplina", example = "30")
        Long id,

        @Schema(description = "Nome da disciplina", example = "Redes de Computadores")
        String name
) {}
