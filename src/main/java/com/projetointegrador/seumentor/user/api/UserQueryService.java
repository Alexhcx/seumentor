package com.projetointegrador.seumentor.user.api;

import java.util.Optional;

import com.projetointegrador.seumentor.user.api.dtos.UserRegister;

public interface UserQueryService {

  Optional<UserRegister> findById(Integer userId);
  Optional<UserRegister> findByEmail(String email);
}
