package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.user.api.UserPreferenceQuery;
import com.projetointegrador.seumentor.user.repository.UserDisciplinePreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component; // Ou @Service
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserPreferenceQueryAdapter implements UserPreferenceQuery {

  private final UserDisciplinePreferenceRepository userPreferenceRepository;

  @Override
  @Transactional(readOnly = true)
  public boolean existsPreferenceForDiscipline(Long disciplineId) {
    return userPreferenceRepository.existsByDisciplineId(disciplineId);
  }
}
