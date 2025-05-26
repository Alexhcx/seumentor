package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;

@Schema(description = "Dados necessários para agendar (solicitar) uma nova mentoria")
public record ScheduleTutoringRequest(
        @NotNull
        @Schema(description = "ID do usuário mentor", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        Long mentorId,

        @NotNull
        @Schema(description = "ID da disciplina da mentoria", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
        Long disciplineId,

        @NotNull
        @Schema(description = "ID do usuário mentorado (quem está solicitando)", example = "52", requiredMode = Schema.RequiredMode.REQUIRED)
        Long menteeId,

        
        @Schema(description = "Tópico ou dúvida principal para a mentoria", example = "Revisão para a prova de Cálculo II", requiredMode = Schema.RequiredMode.REQUIRED)
        String topic,

        @NotNull
        @Schema(description = "Data da mentoria (Formato AAAA-MM-DD)", type = "string", example = "2024-05-20", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate tutoringDate,

        @NotNull
        @Schema(description = "Horário de início da mentoria (Formato HH:mm:ss)", type = "string", example = "14:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalTime startTime,

        @NotNull
        @Schema(description = "Horário de término da mentoria (Formato HH:mm:ss)", type = "string", example = "15:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalTime endTime,

        @NotNull
        @Schema(description = "Tipo da mentoria (ONLINE ou PRESENCIAL)", example = "ONLINE", requiredMode = Schema.RequiredMode.REQUIRED)
        TutoringClassType tutoringClassType
) {}
