package com.projetointegrador.seumentor.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projetointegrador.seumentor.user.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Optional<User> findByPasswordResetToken(String token);

}
