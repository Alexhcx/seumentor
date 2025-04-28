package com.projetointegrador.seumentor.course.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Representação dos dados de uma Área de Curso")
public record CourseAreaRepresentation(
        @Schema(description = "ID único da Área de Curso", example = "5")
        Long id,

        @Schema(description = "Nome do curso", example = "Engenharia Elétrica")
        String course,

        @Schema(description = "Nome da área dentro do curso", example = "Circuitos Elétricos")
        String area,

        @Schema(description = "Data e hora de criação do registro", example = "2023-10-27T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Data e hora da última atualização do registro", example = "2023-10-27T11:30:00")
        LocalDateTime updatedAt
) {}