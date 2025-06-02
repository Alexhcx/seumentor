package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento disparado quando uma mentoria é cancelada.")
public record MentorshipCancelledEvent(
    @Schema(description = "E-mail do destinatário da notificação (mentor ou mentorado)", example = "usuario@example.com")
    String emailDestinatario,

    @Schema(description = "Nome do destinatário da notificação", example = "Carlos")
    String nomeDestinatario,

    @Schema(description = "Nome da disciplina da mentoria cancelada", example = "Álgebra Linear")
    String nomeDisciplina,

    @Schema(description = "Data formatada da mentoria que foi cancelada", example = "15/07/2024")
    String dataMentoria,

    @Schema(description = "Horário de início formatado da mentoria cancelada", example = "10:00")
    String horarioInicioMentoria,

    @Schema(description = "ID da Mentoria que foi cancelada", example = "104")
    Long tutoringId,

    @Schema(description = "Papel de quem cancelou ou contexto (ex: 'pelo mentor', 'pelo administrador', 'por falta de confirmação'). Pode ser nulo.", example = "pelo mentor")
    String motivoAdicional // Um campo mais genérico para adicionar contexto
) {
}
