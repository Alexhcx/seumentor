package com.projetointegrador.seumentor.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projetointegrador.seumentor.user.model.UserFavoriteDisciplines;

import java.util.Optional;

@Repository
public interface UserFavoriteDisciplinesRepository extends JpaRepository<UserFavoriteDisciplines, Integer>{
  boolean existsByUserIdAndDisciplineId(Long userId, Long disciplineId);

  Optional<UserFavoriteDisciplines> findByUserIdAndDisciplineId(Long userId, Long disciplineId);
}
