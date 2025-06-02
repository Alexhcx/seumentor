package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.common.util.CPFUtils;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.*;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projetointegrador.seumentor.user.api.UserCommand;
import com.projetointegrador.seumentor.user.api.events.PasswordResetRequestedEvent;
import com.projetointegrador.seumentor.user.api.events.UserRegisteredEvent;
import com.projetointegrador.seumentor.user.enums.Role;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import com.projetointegrador.seumentor.user.model.User;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;

@Service
@RequiredArgsConstructor
public class UserCommandService implements UserCommand {

  private final UserRepository userRepository;
  private final UserQuery userQuery;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher eventPublisher;

  private static final long DEFAULT_TOKEN_EXPIRY_HOURS = 24;
  private static final Logger log = LoggerFactory.getLogger(UserCommandService.class);

  @Override
  @Transactional
  public UserRepresentation createUser(UserRegistrationRequest request) throws Exception {
    log.info("Command: Attempting to create user with email: {}", request.email());
    if (userRepository.existsByEmail(request.email())) {
      log.warn("Command: Registration failed: Email already exists - {}", request.email());
      throw new Exception("Email já cadastrado: " + request.email());
    }

    Role userRole;
    try {
      userRole = Role.valueOf(request.role().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Command: Registration failed: Invalid role specified - {}", request.role());
      throw new Exception("Role inválida: " + request.role());
    }

    User newUser = new User();
    newUser.setFirstName(request.firstName());
    newUser.setLastName(request.lastName());
    newUser.setEmail(request.email());
    newUser.setCpf(CPFUtils.removerFormatacao(request.cpf()));
    newUser.setPhone(request.phone());
    newUser.setPassword(passwordEncoder.encode(request.password()));
    newUser.setRole(userRole);
    newUser.setPasswordResetToken(null);
    newUser.setPasswordResetTokenExpiry(null);

    User savedUser = userRepository.save(newUser);
    log.info("Command: User entity created successfully with ID: {}", savedUser.getId());

    try {
      UserRegisteredEvent event = new UserRegisteredEvent(
          savedUser.getId(),
          savedUser.getFirstName(),
          savedUser.getEmail());
      eventPublisher.publishEvent(event);
      log.info("Command: UserRegisteredEvent published for user ID: {}", savedUser.getId());
    } catch (Exception e) {
      log.error("Command: Failed to publish UserRegisteredEvent for user ID {}: {}", savedUser.getId(), e.getMessage(),
          e);
    }

    return userQuery.findById(savedUser.getId())
        .orElseThrow(() -> new IllegalStateException(
            "Falha ao buscar representação do usuário recém-criado: " + savedUser.getId()));
  }

  @Transactional
  public UserRepresentation updateUser(Long userId, UserUpdateRequest request) {
    log.info("Command: Attempting to update user with ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("Command: Update failed: User not found with ID: {}", userId);
          return new UserNotFoundException("Usuário não encontrado para atualização com ID: " + userId);
        });

    Optional.ofNullable(request.profileImg()).ifPresent(user::setProfileImg);
    Optional.ofNullable(request.birthday()).ifPresent(user::setBirthday);
    Optional.ofNullable(request.phone()).ifPresent(user::setPhone);
    Optional.ofNullable(request.city()).ifPresent(user::setCity);
    Optional.ofNullable(request.state()).ifPresent(user::setState);
    Optional.ofNullable(request.country()).ifPresent(user::setCountry);
    Optional.ofNullable(request.courseName()).ifPresent(user::setCourseName);
    Optional.ofNullable(request.semester()).ifPresent(user::setSemester);
    Optional.ofNullable(request.university()).ifPresent(user::setUniversity);

    User updatedUser = userRepository.save(user);
    log.info("Command: User entity updated successfully with ID: {}", updatedUser.getId());

    return userQuery.findById(updatedUser.getId())
        .orElseThrow(() -> new IllegalStateException(
            "Falha ao buscar representação do usuário recém-atualizado: " + updatedUser.getId()));
  }

  @Transactional
  public void deleteUser(Long userId) {
    log.info("Command: Attempting to delete user with ID: {}", userId);
    if (!userRepository.existsById(userId)) {
      log.warn("Command: Delete failed: User not found with ID: {}", userId);
      throw new UserNotFoundException("Usuário não encontrado para exclusão com ID: " + userId);
    }
    userRepository.deleteById(userId);
    log.info("Command: User deleted successfully with ID: {}", userId);
  }

  @Override
  @Transactional
  public void requestPasswordReset(String email) throws Exception {
    log.info("Command: Password reset requested for email: {}", email);
    User user = userRepository.findByEmail(email)
        .orElse(null);

    if (user == null) {
      log.warn("Command: Password reset requested for non-existent email: {}", email);
      return;
    }

    String token = UUID.randomUUID().toString();
    LocalDateTime expiryDate = LocalDateTime.now().plusHours(DEFAULT_TOKEN_EXPIRY_HOURS);

    user.setPasswordResetToken(token);
    user.setPasswordResetTokenExpiry(expiryDate);
    userRepository.save(user);
    log.info("Command: Password reset token generated for user ID: {}", user.getId());

    try {
      PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
          user.getEmail(),
          user.getFirstName(),
          token);
      eventPublisher.publishEvent(event);
      log.info("Command: PasswordResetRequestedEvent published for email: {}", email);
    } catch (Exception e) {
      log.error("Command: Failed to publish PasswordResetRequestedEvent for email {}: {}", email, e.getMessage(), e);
    }
  }

  @Override
  @Transactional
  public SetProfileImgIdRequest setProfileImgId(Long userId, SetProfileImgIdRequest request) {
    log.info("Command: Attempting to set profileImgId for user ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("Command: Set profileImgId failed: User not found with ID: {}", userId);
          return new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        });

    user.setProfileImgId(request.profileImgId());
    userRepository.save(user);
    log.info("Command: ProfileImgId {} set successfully for user ID: {}", request.profileImgId(), userId);
    return request; 
  }

  @Override
  @Transactional
  public void resetPassword(String token, String newPassword) throws Exception {
    log.info("Command: Attempting password reset with token (token not logged)");
    if (token == null || token.isEmpty()) {
      log.warn("Command: Password reset failed: Token was null or empty");
      throw new Exception("Token de redefinição inválido ou ausente.");
    }

    User user = userRepository.findByPasswordResetToken(token)
        .orElseThrow(() -> {
          log.warn("Command: Password reset failed: Invalid token provided (token not logged)");
          return new Exception("Token de redefinição inválido ou expirado.");
        });

    if (user.getPasswordResetTokenExpiry() == null
        || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {

      user.setPasswordResetToken(null);
      user.setPasswordResetTokenExpiry(null);
      userRepository.save(user);
      log.warn("Command: Password reset failed: Token expired for user ID: {}", user.getId());
      throw new Exception("Token de redefinição inválido ou expirado.");
    }

    if (newPassword == null || newPassword.length() < 8) {
      log.warn("Password reset failed: New password does not meet criteria for user ID: {}", user.getId());
      throw new Exception("A nova senha deve ter pelo menos 8 caracteres.");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setPasswordResetToken(null);
    user.setPasswordResetTokenExpiry(null);
    userRepository.save(user);
    log.info("Command: Password successfully reset for user ID: {}", user.getId());
  }

  @Override
  @Transactional
  public void changeUserPassword(Long userId, ChangePasswordRequest request) throws Exception {
    log.info("Command: Attempting to change password for user ID: {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("Command: Change password failed: User not found with ID: {}", userId);
          return new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        });

    if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
      log.warn("Command: Change password failed: Old password does not match for user ID: {}", userId);
      throw new BadCredentialsException("A senha antiga está incorreta.");
    }

    if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
      log.warn("Command: Change password failed: New password is the same as the old password for user ID: {}", userId);
      throw new IllegalArgumentException("A nova senha não pode ser igual à senha antiga.");
    }

    if (request.newPassword() == null || request.newPassword().length() < 8) {
      log.warn("Command: Change password failed: New password does not meet length criteria for user ID: {}", userId);
      throw new IllegalArgumentException("A nova senha deve ter pelo menos 8 caracteres.");
    }

    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    log.info("Command: Password successfully changed for user ID: {}", userId);
  }

  @Transactional
  public void promoteToMentor(Long userId) {
    log.info("Command: Attempting to promote user ID {} to MENTOR", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("Command: Promote to mentor failed: User not found with ID {}", userId);
          return new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        });

    if (user.getRole() != Role.MENTOR) {
      user.setRole(Role.MENTOR);
      userRepository.save(user);
      log.info("Command: User ID {} successfully promoted to MENTOR", userId);
    } else {
      log.info("Command: User ID {} is already a MENTOR. No change made.", userId);
    }
  }
}