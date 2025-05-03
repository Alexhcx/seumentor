package com.projetointegrador.seumentor.course.api;

import java.util.Optional;

import com.projetointegrador.seumentor.course.model.Discipline; 

import com.projetointegrador.seumentor.course.api.dto.SimpleDisciplineRepresentation;

public interface DisciplineQuery {

    boolean existsById(Long disciplineId);

    Optional<SimpleDisciplineRepresentation> findBasicInfoById(Long disciplineId);

    Discipline getReferenceById(Long disciplineId);
}
