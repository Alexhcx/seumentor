package com.projetointegrador.seumentor.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projetointegrador.seumentor.user.model.UserModel;

public interface UserRepository extends JpaRepository<UserModel, Integer> {

  Optional<UserModel> findByEmail(String email);

  boolean existsByEmail(String email);

}
