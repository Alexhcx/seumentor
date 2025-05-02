package com.projetointegrador.seumentor.security.auth;

public record AuthenticationRequest(
        String email,
        String password
) {
}
