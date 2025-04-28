package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Representação da disponibilidade de um mentor para uma disciplina específica")
public record MentorDisciplineAvailabilityRepresentation(
        @Schema(description = "Nome da disciplina", example = "Cálculo I")
        String disciplineName,

        @Schema(description = "Lista de horários disponíveis para esta disciplina")
        List<AvailabilitySlot> slots
) {}
