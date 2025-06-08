package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento disparado quando um novo participante entra em uma mentoria existente.")
public record ParticipantJoinedEvent(
    @Schema(description = "E-mail do mentor a ser notificado", example = "mentor@example.com")
    String emailMentor,

    @Schema(description = "Nome do mentor", example = "Carlos Andrade")
    String nomeMentor,

    @Schema(description = "Nome do novo participante que entrou", example = "João da Silva")
    String nomeNovoParticipante,

    @Schema(description = "Nome da disciplina da mentoria", example = "Cálculo II")
    String nomeDisciplina,

    @Schema(description = "ID da Mentoria", example = "151")
    Long tutoringId
) {}
