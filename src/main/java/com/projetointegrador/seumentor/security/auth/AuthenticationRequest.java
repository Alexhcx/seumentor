package com.projetointegrador.seumentor.security.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticationRequest(
        @Email String email,
        @NotBlank
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password
) {
}
