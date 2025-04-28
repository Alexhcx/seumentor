package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.user.api.UserAvailabilityQuery;
import com.projetointegrador.seumentor.user.repository.MentorAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component; // Ou @Service
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserAvailabilityQueryAdapter implements UserAvailabilityQuery {

  private final MentorAvailabilityRepository mentorAvailabilityRepository;

  @Override
  @Transactional(readOnly = true)
  public boolean existsAvailabilityForDiscipline(Long disciplineId) {
    return mentorAvailabilityRepository.existsByDisciplineId(disciplineId);
  }
}
