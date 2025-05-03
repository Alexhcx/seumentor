package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados necessários para registrar um novo usuário no sistema")
public record UserRegistrationRequest(
        @Schema(description = "Primeiro nome do usuário", example = "Ana", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        @Schema(description = "Sobrenome do usuário", example = "Souza", requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,

        @Schema(description = "Endereço de e-mail do usuário (será usado para login)", example = "ana.souza@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "Número de CPF do usuário (apenas dígitos)", example = "12345678901", requiredMode = Schema.RequiredMode.REQUIRED)
        String cpf,

        @Schema(description = "Número de telefone do usuário (apenas dígitos)", example = "11987654321", requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @Schema(description = "Senha para a conta do usuário", example = "senhaSegura123", requiredMode = Schema.RequiredMode.REQUIRED)
        String password,

        @Schema(description = "Papel/Permissão inicial do usuário (USER ou ADMIN)", example = "USER", requiredMode = Schema.RequiredMode.REQUIRED)
        String role
) {}
