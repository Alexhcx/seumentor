package com.projetointegrador.seumentor.user.api.dtos;

public record UserUpdateRequest(
    String firstName,
    String lastName,
    String profileImg,
    String birthday,
    String city,
    String state,
    String country,
    String courseName,
    String semester,
    String university
) {}
