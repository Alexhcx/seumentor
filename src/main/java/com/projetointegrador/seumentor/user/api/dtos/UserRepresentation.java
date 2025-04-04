package com.projetointegrador.seumentor.user.api.dtos;

public record UserRepresentation(
    Integer id,
    String firstName,
    String lastName,
    String email,
    String profileImg,
    String birthday,
    String city,
    String state,
    String country,
    String courseName,
    String semester,
    String university
) {}
