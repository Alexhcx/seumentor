package com.projetointegrador.seumentor.course.service;

import com.projetointegrador.seumentor.course.api.DisciplineQuery;
import com.projetointegrador.seumentor.course.api.dto.SimpleDisciplineRepresentation;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.course.repository.DisciplineRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DisciplineQueryAdapter implements DisciplineQuery {

    private final DisciplineRepository disciplineRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long disciplineId) {
        if (disciplineId == null) {
            return false;
        }
        return disciplineRepository.existsById(disciplineId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SimpleDisciplineRepresentation> findBasicInfoById(Long disciplineId) {
        if (disciplineId == null) {
            return Optional.empty();
        }
        return disciplineRepository.findById(disciplineId)
                .map(discipline -> new SimpleDisciplineRepresentation(discipline.getId(), discipline.getDisciplineName()));
    }

    @Override
    @Transactional
    public Discipline getReferenceById(Long disciplineId) {

        if (disciplineId == null) {
            throw new IllegalArgumentException("Discipline ID cannot be null for getReferenceById");
        }
        return disciplineRepository.getReferenceById(disciplineId);
    }

}
