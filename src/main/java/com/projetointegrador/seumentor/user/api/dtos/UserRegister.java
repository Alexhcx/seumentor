package com.projetointegrador.seumentor.user.api.dtos;

public record UserRegister(
  Integer id,
  String firstName,
  String lastName,
  String email
) {}
