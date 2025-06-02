package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserProfileImgIdRepresentation;
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
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserQueryAdapter implements UserQuery {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png");

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
    public UserProfileImgIdRepresentation findProfileImgIdByUserId(Long userId) { // Renamed to match your snippet
        log.debug("Adapter: Finding profileImgId and extension by user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Adapter: User not found with ID: {} when trying to find profileImgId", userId);
                    return new UserNotFoundException("Usuário não encontrado com ID: " + userId);
                });

        String profileImgUrl = user.getProfileImg();
        String fileExtension = null;

        if (StringUtils.hasText(profileImgUrl)) {
            String pathPart = profileImgUrl;
            int queryParamStartIndex = pathPart.indexOf('?');
            if (queryParamStartIndex != -1) {
                pathPart = pathPart.substring(0, queryParamStartIndex);
            }

            int lastDotIndex = pathPart.lastIndexOf('.');
            if (lastDotIndex != -1 && lastDotIndex < pathPart.length() - 1) {
                String extractedExt = pathPart.substring(lastDotIndex).toLowerCase(); 
                if (ALLOWED_EXTENSIONS.contains(extractedExt)) {
                    fileExtension = extractedExt;
                    log.debug("Adapter: Extracted extension {} for user ID: {}", fileExtension, userId);
                } else {
                    log.warn("Adapter: Extracted extension {} is not in allowed list for user ID: {}. URL: {}",
                            extractedExt, userId, profileImgUrl);
                }
            } else {
                log.warn("Adapter: No valid extension found in profileImg URL for user ID: {}. URL: {}", userId,
                        profileImgUrl);
            }
        } else {
            log.info("Adapter: User with ID: {} does not have a profileImg URL set.", userId);
        }

        UUID profileImgId = user.getProfileImgId();
        if (profileImgId == null) {
            log.info("Adapter: User with ID: {} does not have a profileImgId UUID set.", userId);
        }

        return new UserProfileImgIdRepresentation(profileImgId, fileExtension);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRepresentation> findAllUserRepresentations() {
        log.debug("Adapter: Finding all user representations");
        List<User> users = userRepository.findAll();
        return userMapper.toRepresentationList(users);
    }
}