package com.projetointegrador.seumentor.user.api;

import java.util.Optional;

import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;

public interface UserQuery {
  // Optional<UserRegister> findById(Integer userId);
  // Optional<UserRegister> findByEmail(String email);
  Optional<UserRepresentation> findById(Integer userId);
  Optional<UserRepresentation> findByEmail(String email);
}
