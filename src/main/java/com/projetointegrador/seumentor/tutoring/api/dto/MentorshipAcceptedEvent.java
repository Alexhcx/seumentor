package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento disparado quando uma solicitação de mentoria é aceita pelo mentor.")
public record MentorshipAcceptedEvent(
    @Schema(description = "E-mail do usuário (mentorado) que solicitou a mentoria", example = "mentorado@example.com") String emailMentorado,

    @Schema(description = "Primeiro nome do usuário (mentorado)", example = "João") String nomeMentorado,

    @Schema(description = "Nome completo do mentor que aceitou", example = "Maria Silva") String nomeMentor,

    @Schema(description = "Nome da disciplina da mentoria", example = "Cálculo I") String nomeDisciplina,

    @Schema(description = "ID da Mentoria", example = "101") Long tutoringId) {
}
