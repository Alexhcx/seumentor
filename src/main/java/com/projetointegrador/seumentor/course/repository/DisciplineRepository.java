package com.projetointegrador.seumentor.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projetointegrador.seumentor.course.model.Discipline;

public interface DisciplineRepository extends JpaRepository<Discipline, Integer> {

}
