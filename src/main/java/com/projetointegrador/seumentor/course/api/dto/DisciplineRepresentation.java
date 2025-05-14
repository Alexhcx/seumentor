package com.projetointegrador.seumentor.course.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação dos dados de uma Disciplina")
public record DisciplineRepresentation(
        @Schema(description = "ID único da Disciplina", example = "1")
        Long disciplineId,

        @Schema(description = "Nome da disciplina", example = "ALGORITMOS")
        String disciplineName,

        @Schema(description = "Descrição da disciplina", example = "Introdução à lógica de programação e construção de algoritmos.")
        String description,

        @Schema(description = "ID da Área de Curso associada", example = "1")
        Long courseAreaId,

        @Schema(description = "Área do curso", example = "BÁSICO I")
        String area,

        @Schema(description = "Nome do curso", example = "Análise e Desenvolvimento de Sistemas")
        String courseName
) {}
