package com.projetointegrador.seumentor.user.api;

public interface UserAvailabilityQuery {
  boolean existsAvailabilityForDiscipline(Long disciplineId);
}