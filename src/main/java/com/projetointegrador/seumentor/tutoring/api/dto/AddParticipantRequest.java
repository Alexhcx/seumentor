package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados necessários para adicionar um participante a uma mentoria existente")
public record AddParticipantRequest(
        @NotNull
        @Schema(description = "ID do usuário que deseja participar da mentoria", example = "52", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @NotBlank
        @Schema(description = "Tópico ou dúvida principal que o participante deseja abordar na mentoria", example = "Dificuldade com JOINs em SQL", requiredMode = Schema.RequiredMode.REQUIRED)
        String topic
) {}
