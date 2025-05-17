package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

import com.projetointegrador.seumentor.common.enums.DayWeek;

@Schema(description = "Representação de um único slot de horário de disponibilidade")
public record AvailabilitySlotRepresentation(
        @Schema(description = "Dia da semana da disponibilidade", example = "SEGUNDA_FEIRA")
        DayWeek dayOfWeek,

        @Schema(description = "Horário de início da disponibilidade", type = "string", example = "14:30:00")
        LocalTime startTime,

        @Schema(description = "Horário de término da disponibilidade", type = "string", example = "16:00:00")
        LocalTime endTime
) {}
