package com.projetointegrador.seumentor.user.api;

import java.util.List;
import java.util.Optional;

import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.model.User; // Necessário para getUserReferenceById
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
}