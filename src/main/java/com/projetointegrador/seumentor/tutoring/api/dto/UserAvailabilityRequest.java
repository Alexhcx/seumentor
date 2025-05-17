package com.projetointegrador.seumentor.tutoring.api.dto;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Schema(description = "Dados para adicionar um novo horário de disponibilidade para um usuário/mentor")
public record UserAvailabilityRequest(
        @NotNull
        @Schema(description = "ID da disciplina para a qual a disponibilidade se aplica", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        Long disciplineId,

        @NotNull
        @Schema(description = "Dia da semana para a disponibilidade", example = "SEGUNDA_FEIRA", requiredMode = Schema.RequiredMode.REQUIRED)
        DayWeek dayOfWeek,

        @NotNull
        @Schema(description = "Horário de início da disponibilidade (Formato HH:mm:ss)", type = "string", example = "09:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalTime startTime,

        @NotNull
        @Schema(description = "Horário de término da disponibilidade (Formato HH:mm:ss)", type = "string", example = "11:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalTime endTime,

        @NotNull
        @Schema(description = "Tipo da aula da monitoria (ONLINE ou PRESENCIAL)", example = "ONLINE", requiredMode = Schema.RequiredMode.REQUIRED)
        TutoringClassType tutoringClassType
) {}
