package com.projetointegrador.seumentor.user.api.dtos;

public record UserRegistrationRequest(
    String firstName,
    String lastName,
    String email,
    String password,
    String role 
) {}
