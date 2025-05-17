package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Representação da disponibilidade de um mentor para uma disciplina específica")
public record MentorDisciplineAvailabilityRepresentation(
        @Schema(description = "Nome da disciplina", example = "Cálculo I")
        String disciplineName,

        @Schema(description = "Nome do curso ao qual a disciplina desta disponibilidade pertence", example = "Análise e Desenvolvimento de Sistemas")
        String courseName,

        @Schema(description = "Lista de horários disponíveis para esta disciplina")
        List<AvailabilitySlotRepresentation> slots
) {}
