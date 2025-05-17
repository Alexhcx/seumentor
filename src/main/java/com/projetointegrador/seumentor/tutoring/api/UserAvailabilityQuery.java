package com.projetointegrador.seumentor.tutoring.api;

public interface UserAvailabilityQuery {
  boolean existsAvailabilityForDiscipline(Long disciplineId);
}