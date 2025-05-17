package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.api.mapper.UserMapper; 
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class UserQueryAdapter implements UserQuery {

    private final UserRepository userRepository;
    private final UserMapper userMapper; 

    private static final Logger log = LoggerFactory.getLogger(UserQueryAdapter.class);

    @Autowired
    public UserQueryAdapter(UserRepository userRepository, UserMapper userMapper) { 
        this.userRepository = userRepository;
        this.userMapper = userMapper; 
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<UserRepresentation> findById(Long userId) {
        log.debug("Adapter: Finding user by ID: {}", userId);
        return userRepository.findById(userId).map(userMapper::toRepresentation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRepresentation> findByEmail(String email) {
        log.debug("Adapter: Finding user by email: {}", email);
        return userRepository.findByEmail(email).map(userMapper::toRepresentation);
    }

    @Override
    @Transactional
    public User getUserReferenceById(Long userId) {
        log.debug("Adapter: Getting user reference by ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.warn("Adapter: User reference requested for non-existent ID: {}", userId);
            throw new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        }
        return userRepository.getReferenceById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRepresentation> findAllUserRepresentations() {
        log.debug("Adapter: Finding all user representations");
        List<User> users = userRepository.findAll();
        return userMapper.toRepresentationList(users); 
    }
}