package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQuery {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public Optional<UserRepresentation> findById(Long userId) {
        return userRepository.findById(userId).map(this::mapToRepresentation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRepresentation> findByEmail(String email) {
        return userRepository.findByEmail(email).map(this::mapToRepresentation);
    }

    @Override
    @Transactional
    public User getUserReferenceById(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Usuário não encontrado com ID: " + userId);
        }
        return userRepository.getReferenceById(userId);
    }

    private UserRepresentation mapToRepresentation(User user) {
        return new UserRepresentation(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getProfileImg(),
                user.getBirthday(),
                user.getCpf(),
                user.getPhone(),
                user.getCity(),
                user.getState(),
                user.getCountry(),
                user.getCourseName(),
                user.getSemester(),
                user.getUniversity()
        );
    }
}
