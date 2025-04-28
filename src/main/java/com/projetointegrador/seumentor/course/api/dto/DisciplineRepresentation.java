package com.projetointegrador.seumentor.course.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Representação dos dados de uma Disciplina")
public record DisciplineRepresentation(
        @Schema(description = "ID único da Disciplina", example = "25")
        Long id,

        @Schema(description = "Nome da disciplina", example = "Banco de Dados II")
        String disciplineName,

        @Schema(description = "Descrição da disciplina", example = "Modelagem de dados, SQL avançado, NoSQL.")
        String description,

        @Schema(description = "ID da Área de Curso associada", example = "8")
        Long courseAreaId,

        @Schema(description = "Nome completo da Área de Curso associada (Curso - Área)", example = "Sistemas de Informação - Banco de Dados")
        String courseAreaName,

        @Schema(description = "Data e hora de criação do registro", example = "2023-11-01T09:15:00")
        LocalDateTime createdAt,

        @Schema(description = "Data e hora da última atualização do registro", example = "2023-11-05T14:00:00")
        LocalDateTime updatedAt
) {}
