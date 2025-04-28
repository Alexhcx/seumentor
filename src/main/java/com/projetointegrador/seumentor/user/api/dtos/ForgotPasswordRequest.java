package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados necessários para solicitar a redefinição de senha")
public record ForgotPasswordRequest(
        @Schema(description = "E-mail do usuário que esqueceu a senha", example = "usuario.esquecido@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email
) {}
