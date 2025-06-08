package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento disparado quando uma nova mentoria é solicitada por um aluno para um mentor.")
public record MentorshipRequestedEvent(
    @Schema(description = "E-mail do mentor a ser notificado", example = "mentor@example.com")
    String emailMentor,

    @Schema(description = "Nome do mentor", example = "Carlos Andrade")
    String nomeMentor,

    @Schema(description = "Nome do aluno que solicitou a mentoria", example = "Maria Oliveira")
    String nomeMentorado,

    @Schema(description = "Nome da disciplina da mentoria", example = "Cálculo II")
    String nomeDisciplina,

    @Schema(description = "ID da Mentoria que foi solicitada", example = "151")
    Long tutoringId
) {}
