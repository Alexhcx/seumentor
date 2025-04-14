package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component; // Ou @Service
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TutoringQueryAdapter implements TutoringQuery {

    private final TutoringRepository tutoringRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsTutoringForDiscipline(Long disciplineId) {
        return tutoringRepository.existsByDisciplineId(disciplineId);
    }
}