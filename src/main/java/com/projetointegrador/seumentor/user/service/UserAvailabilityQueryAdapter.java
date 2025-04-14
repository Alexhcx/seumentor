package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.user.api.UserAvailabilityQuery;
import com.projetointegrador.seumentor.user.repository.UserAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component; // Ou @Service
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserAvailabilityQueryAdapter implements UserAvailabilityQuery {

  private final UserAvailabilityRepository userAvailabilityRepository;

  @Override
  @Transactional(readOnly = true)
  public boolean existsAvailabilityForDiscipline(Long disciplineId) {
    return userAvailabilityRepository.existsByDisciplineId(disciplineId);
  }
}
