package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.tutoring.api.UserAvailabilityQuery;
import com.projetointegrador.seumentor.tutoring.repository.MentorAvailabilityRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
