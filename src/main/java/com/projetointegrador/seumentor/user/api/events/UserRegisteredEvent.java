package com.projetointegrador.seumentor.user.api.events;

public record UserRegisteredEvent(
    Integer userId,
    String firstName,
    String email
) {}
