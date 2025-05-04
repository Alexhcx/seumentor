// main/java/com/projetointegrador/seumentor/user/api/UserQuery.java
package com.projetointegrador.seumentor.user.api;

import java.util.List; // Importar List
import java.util.Optional;

import com.projetointegrador.seumentor.user.api.dtos.MentorProfileRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserAvailabilityRepresentation; // Importar
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.model.User;
import org.springframework.transaction.annotation.Transactional;

public interface UserQuery {

  @Transactional(readOnly = true)
  Optional<UserRepresentation> findById(Long userId);

  @Transactional(readOnly = true)
  Optional<UserRepresentation> findByEmail(String email);

  @Transactional
  User getUserReferenceById(Long userId);

  @Transactional(readOnly = true)
  List<UserRepresentation> findAllUserRepresentations();

  @Transactional(readOnly = true)
  Optional<UserAvailabilityRepresentation> findAvailabilityRepresentationById(Long availabilityId);

  @Transactional(readOnly = true)
  List<UserAvailabilityRepresentation> findAvailabilitiesRepresentationByUserId(Long userId);

  @Transactional(readOnly = true)
  Optional<MentorProfileRepresentation> findMentorProfileById(Long mentorId);

  @Transactional(readOnly = true)
  List<MentorProfileRepresentation> findAllMentorProfiles();

}