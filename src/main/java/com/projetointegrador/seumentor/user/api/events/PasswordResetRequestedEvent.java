package com.projetointegrador.seumentor.user.api.events;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento disparado quando um usuário solicita a redefinição de senha. Contém os dados necessários para enviar o e-mail de redefinição.")
public record PasswordResetRequestedEvent(
        @Schema(description = "E-mail do usuário que solicitou a redefinição", example = "usuario.esquecido@example.com")
        String email,

        @Schema(description = "Primeiro nome do usuário, para personalizar o e-mail", example = "Carlos")
        String firstName,

        @Schema(description = "Token único gerado para a redefinição de senha", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
        String token
) {}
