package com.projetointegrador.seumentor.course.api;

import java.util.Optional;

import com.projetointegrador.seumentor.course.model.Discipline; 

import com.projetointegrador.seumentor.course.api.dto.SimpleDisciplineInfo;

public interface DisciplineQuery {

    boolean existsById(Long disciplineId);

    Optional<SimpleDisciplineInfo> findBasicInfoById(Long disciplineId);

    Discipline getReferenceById(Long disciplineId);
}
