package com.projetointegrador.seumentor.user.api.dtos;

public record UserRepresentation(
    Integer id,
    String firstName,
    String lastName,
    String email

) {}
