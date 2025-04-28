package com.projetointegrador.seumentor.user.api.dtos;

import com.projetointegrador.seumentor.user.model.DayWeek;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "Representação de um horário de disponibilidade de um usuário/mentor")
public record UserAvailabilityRepresentation(
        @Schema(description = "ID único da disponibilidade", example = "101")
        Long id,

        @Schema(description = "Informações básicas da disciplina associada")
        SimpleDisciplineRepresentation discipline,

        @Schema(description = "Dia da semana da disponibilidade", example = "TERCA_FEIRA")
        DayWeek dayOfWeek,

        @Schema(description = "Horário de início da disponibilidade", type = "string", example = "14:30:00")
        LocalTime startTime,

        @Schema(description = "Horário de término da disponibilidade", type = "string", example = "16:00:00")
        LocalTime endTime
) {}
