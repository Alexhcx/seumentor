package com.projetointegrador.seumentor.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projetointegrador.seumentor.user.model.UserDisciplinePreference;

@Repository
public interface UserDisciplinePreferenceRepository extends JpaRepository<UserDisciplinePreference, Integer>{

  boolean existsByDisciplineId(Long disciplineId);

}
