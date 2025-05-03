package com.projetointegrador.seumentor.user.api.dtos;

import com.projetointegrador.seumentor.user.model.DayWeek;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

@Schema(description = "Representação de um único slot de horário de disponibilidade")
public record AvailabilitySlotRepresentation(
        @Schema(description = "Dia da semana da disponibilidade", example = "SEGUNDA_FEIRA")
        DayWeek dayOfWeek,

        @Schema(description = "Horário de início da disponibilidade", type = "string", example = "14:30:00")
        LocalTime startTime,

        @Schema(description = "Horário de término da disponibilidade", type = "string", example = "16:00:00")
        LocalTime endTime
) {}
