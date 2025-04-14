package com.projetointegrador.seumentor.user.api.events;

public record UserRegisteredEvent(
    Long userId,
    String firstName,
    String email
) {}
