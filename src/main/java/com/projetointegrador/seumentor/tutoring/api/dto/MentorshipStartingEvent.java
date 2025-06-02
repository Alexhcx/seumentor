package com.projetointegrador.seumentor.tutoring.api.dto;

import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento disparado quando uma mentoria está prestes a começar.")
public record MentorshipStartingEvent(
    @Schema(description = "E-mail do usuário (mentorado) a ser notificado", example = "mentorado@example.com")
    String emailMentorado,

    @Schema(description = "Primeiro nome do usuário (mentorado)", example = "João")
    String nomeMentorado,

    @Schema(description = "Nome completo do mentor", example = "Maria Silva")
    String nomeMentor,

    @Schema(description = "Nome da disciplina da mentoria", example = "Cálculo I")
    String nomeDisciplina,

    @Schema(description = "Horário de início formatado da mentoria", example = "14:00")
    String horarioInicio, 

    @Schema(description = "Link para acessar a mentoria (se ONLINE)", example = "https://meet.example.com/xyz")
    String linkMentoria, 

    @Schema(description = "Local da mentoria (se PRESENCIAL)", example = "Sala B203")
    String localMentoria, 

    @Schema(description = "Tipo da mentoria (ONLINE ou PRESENCIAL)")
    TutoringClassType tipoMentoria,

    @Schema(description = "ID da Mentoria", example = "102")
    Long tutoringId
) {
}
