package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

Schema(description = "Evento disparado quando uma mentoria é marcada como concluída.")
public record MentorshipCompletedEvent(
    @Schema(description = "E-mail do usuário (mentorado) a ser notificado para avaliar", example = "mentorado@example.com")
    String emailMentorado,

    @Schema(description = "Primeiro nome do usuário (mentorado)", example = "João")
    String nomeMentorado,

    @Schema(description = "Nome completo do mentor", example = "Maria Silva")
    String nomeMentor,

    @Schema(description = "Nome da disciplina da mentoria", example = "Cálculo I")
    String nomeDisciplina,

    @Schema(description = "ID da Mentoria que foi concluída", example = "103")
    Long tutoringId,

    @Schema(description = "ID do usuário mentorado", example = "52")
    Long menteeId 
) {
}
