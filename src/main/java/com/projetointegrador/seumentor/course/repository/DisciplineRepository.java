package com.projetointegrador.seumentor.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.projetointegrador.seumentor.course.model.Discipline;

@Repository
public interface DisciplineRepository extends JpaRepository<Discipline, Long> {

  boolean existsByCourseAreaId(Long id);

  boolean existsByDisciplineNameAndCourseAreaId(
      String disciplineName,
      Long courseAreaId);

  List<Discipline> findByCourseAreaId(Long courseAreaId);
}
