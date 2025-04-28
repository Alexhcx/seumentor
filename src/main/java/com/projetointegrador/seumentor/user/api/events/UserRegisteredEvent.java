package com.projetointegrador.seumentor.user.api.events;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento disparado após um novo usuário ser registrado com sucesso. Contém informações básicas para ações pós-registro (ex: e-mail de boas-vindas).")
public record UserRegisteredEvent(
        @Schema(description = "ID do usuário recém-criado", example = "123")
        Long userId,

        @Schema(description = "Primeiro nome do usuário recém-criado", example = "Ana")
        String firstName,

        @Schema(description = "E-mail do usuário recém-criado", example = "ana.souza@example.com")
        String email
) {}
