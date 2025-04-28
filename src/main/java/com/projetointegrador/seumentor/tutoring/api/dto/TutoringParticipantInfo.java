package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informações de um participante de uma mentoria")
public record TutoringParticipantInfo(
        @Schema(description = "ID do usuário participante", example = "52")
        Long userId,

        @Schema(description = "Nome completo do usuário participante", example = "Maria Oliveira")
        String userName,

        @Schema(description = "Tópico de interesse do participante na mentoria", example = "Exercícios de limites")
        String topic
) {}