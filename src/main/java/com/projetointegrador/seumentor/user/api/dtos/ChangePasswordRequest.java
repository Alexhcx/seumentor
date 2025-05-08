package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para a troca de senha do usuário logado")
public record ChangePasswordRequest(
        @NotBlank(message = "A senha antiga é obrigatória.")
        @Schema(description = "Senha atual do usuário", example = "senhaAntiga123", requiredMode = Schema.RequiredMode.REQUIRED)
        String oldPassword,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, message = "A nova senha deve ter pelo menos 8 caracteres.")
        @Schema(description = "Nova senha desejada para a conta (mínimo 8 caracteres)", example = "novaSenhaSuperSegura456", requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword
) {}
