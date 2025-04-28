package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados necessários para redefinir a senha usando um token")
public record ResetPasswordRequest(
        @Schema(description = "Token de redefinição recebido por e-mail", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8", requiredMode = Schema.RequiredMode.REQUIRED)
        String token,

        @Schema(description = "Nova senha desejada para a conta", example = "novaSenhaSuperSegura456", requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword
) {}
