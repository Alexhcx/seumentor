package com.projetointegrador.seumentor.user.api.events;

public record PasswordResetRequestedEvent(
    String email,
    String firstName,
    String token
) {}
