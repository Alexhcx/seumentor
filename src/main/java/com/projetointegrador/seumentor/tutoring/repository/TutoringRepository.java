package com.projetointegrador.seumentor.tutoring.repository;

import com.projetointegrador.seumentor.tutoring.model.Tutoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TutoringRepository extends JpaRepository<Tutoring, Long>, JpaSpecificationExecutor<Tutoring> {

  boolean existsByDisciplineId(Long disciplineId);

}
