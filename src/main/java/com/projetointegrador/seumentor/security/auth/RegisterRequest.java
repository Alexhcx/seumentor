package com.projetointegrador.seumentor.security.auth;

public record RegisterRequest (
  String firstName,
  String lastName,
  String email,
  String password
){}
