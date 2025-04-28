package com.projetointegrador.seumentor.user.api;

import java.util.Optional;

import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.model.User;
import org.springframework.transaction.annotation.Transactional;

public interface UserQuery {

  @Transactional(readOnly = true)
  Optional<UserRepresentation> findById(Long userId);

  Optional<UserRepresentation> findByEmail(String email);
  User getUserReferenceById(Long userId);
}
