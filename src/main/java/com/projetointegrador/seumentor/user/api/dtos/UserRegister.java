package com.projetointegrador.seumentor.user.api.dtos;

public record UserRegister(
  Long id,
  String firstName,
  String lastName,
  String email
) {}
