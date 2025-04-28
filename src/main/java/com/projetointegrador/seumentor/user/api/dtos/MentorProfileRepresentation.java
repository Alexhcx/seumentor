package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Representação completa do perfil de um mentor, incluindo suas disponibilidades agrupadas por disciplina")
public record MentorProfileRepresentation(
        @Schema(description = "ID único do mentor", example = "1")
        Long id,

        @Schema(description = "Primeiro nome do mentor", example = "João")
        String firstName,

        @Schema(description = "Sobrenome do mentor", example = "Silva")
        String lastName,

        @Schema(description = "Nome do curso do mentor", example = "Engenharia de Software")
        String courseName,

        @Schema(description = "Lista de disciplinas e horários de disponibilidade do mentor")
        List<MentorDisciplineAvailabilityRepresentation> availabilities
) {}
