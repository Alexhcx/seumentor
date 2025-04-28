package com.projetointegrador.seumentor.user.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representação simplificada do usuário retornada imediatamente após o registro bem-sucedido (Exemplo, pode não ser usado atualmente)")
public record UserRegisterRequest(
        @Schema(description = "ID único do usuário recém-criado", example = "123")
        Long id,

        @Schema(description = "Primeiro nome do usuário", example = "Carlos")
        String firstName,

        @Schema(description = "Sobrenome do usuário", example = "Pereira")
        String lastName,

        @Schema(description = "Endereço de e-mail do usuário", example = "carlos.pereira@example.com")
        String email
) {}