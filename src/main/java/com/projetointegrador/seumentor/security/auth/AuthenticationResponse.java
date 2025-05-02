package com.projetointegrador.seumentor.security.auth;

public record AuthenticationResponse(
        String token,
        Long userId
) {}

