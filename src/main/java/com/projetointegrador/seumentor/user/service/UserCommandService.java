// src/main/java/com/projetointegrador/seumentor/user/service/UserCommandService.java
package com.projetointegrador.seumentor.user.service;

import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projetointegrador.seumentor.user.api.UserCommand;
import com.projetointegrador.seumentor.user.api.dtos.UserRegistrationRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserUpdateRequest;
import com.projetointegrador.seumentor.user.api.events.UserRegisteredEvent;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import com.projetointegrador.seumentor.user.model.Role;
import com.projetointegrador.seumentor.user.model.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;

@Service
@RequiredArgsConstructor
public class UserCommandService implements UserCommand {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher eventPublisher;
  private static final long DEFAULT_TOKEN_EXPIRY_HOURS = 24;
  private static final Logger log = LoggerFactory.getLogger(UserCommandService.class);

  @Override
  @Transactional
  public UserRepresentation createUser(UserRegistrationRequest request) throws Exception {
    log.info("Attempting to create user with email: {}", request.email());
    if (userRepository.existsByEmail(request.email())) {
      log.warn("Registration failed: Email already exists - {}", request.email());
      throw new Exception("Email já cadastrado: " + request.email());
    }

    Role userRole;
    try {
      userRole = Role.valueOf(request.role().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.error("Registration failed: Invalid role specified - {}", request.role());
      throw new Exception("Role inválida: " + request.role());
    }

    User newUser = new User();
    newUser.setFirstName(request.firstName());
    newUser.setLastName(request.lastName());
    newUser.setEmail(request.email());
    newUser.setPassword(passwordEncoder.encode(request.password()));
    newUser.setRole(userRole);

    newUser.setPasswordResetToken(null);
    newUser.setPasswordResetTokenExpiry(null);

    User savedUser = userRepository.save(newUser);
    log.info("User created successfully with ID: {} and email: {}", savedUser.getId(), savedUser.getEmail());

    try {
      UserRegisteredEvent event = new UserRegisteredEvent(
          savedUser.getId(),
          savedUser.getFirstName(),
          savedUser.getEmail());
      eventPublisher.publishEvent(event);
      log.info("UserRegisteredEvent published for user ID: {}", savedUser.getId());
    } catch (Exception e) {
      log.error("Failed to publish UserRegisteredEvent for user ID {}: {}", savedUser.getId(), e.getMessage(), e);
    }

    return mapToRepresentation(savedUser);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public UserRepresentation getUserById(Integer userId) {
    log.debug("Attempting to find user with ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("User not found with ID: {}", userId);
          return new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        });
    log.debug("User found with ID: {}", userId);
    return mapToRepresentation(user);
  }

  @Transactional(Transactional.TxType.SUPPORTS)
  public List<UserRepresentation> getAllUsers() {
    log.debug("Attempting to retrieve all users");
    List<User> users = userRepository.findAll();
    log.info("Retrieved {} users", users.size());
    return users.stream()
        .map(this::mapToRepresentation)
        .collect(Collectors.toList());
  }

  @Transactional
  public UserRepresentation updateUser(Integer userId, UserUpdateRequest request) {
    log.info("Attempting to update user with ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("Update failed: User not found with ID: {}", userId);
          return new UserNotFoundException("Usuário não encontrado para atualização com ID: " + userId);
        });

    Optional.ofNullable(request.firstName()).ifPresent(user::setFirstName);
    Optional.ofNullable(request.lastName()).ifPresent(user::setLastName);
    Optional.ofNullable(request.profileImg()).ifPresent(user::setProfileImg);
    Optional.ofNullable(request.birthday()).ifPresent(user::setBirthday);
    Optional.ofNullable(request.city()).ifPresent(user::setCity);
    Optional.ofNullable(request.state()).ifPresent(user::setState);
    Optional.ofNullable(request.country()).ifPresent(user::setCountry);
    Optional.ofNullable(request.courseName()).ifPresent(user::setCourseName);
    Optional.ofNullable(request.semester()).ifPresent(user::setSemester);
    Optional.ofNullable(request.university()).ifPresent(user::setUniversity);

    User updatedUser = userRepository.save(user);
    log.info("User updated successfully with ID: {}", updatedUser.getId());
    return mapToRepresentation(updatedUser);
  }

  @Transactional
  public void deleteUser(Integer userId) {
    log.info("Attempting to delete user with ID: {}", userId);
    if (!userRepository.existsById(userId)) {
      log.warn("Delete failed: User not found with ID: {}", userId);
      throw new UserNotFoundException("Usuário não encontrado para exclusão com ID: " + userId);
    }
    userRepository.deleteById(userId);
    log.info("User deleted successfully with ID: {}", userId);
  }

  @Override
  @Transactional
  public void requestPasswordReset(String email) throws Exception {
    log.info("Password reset requested for email: {}", email);
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> {
          log.warn("Password reset requested for non-existent email: {}", email);
          return new Exception("Usuário não encontrado com o email: " + email);
        });

    String token = UUID.randomUUID().toString();
    LocalDateTime expiryDate = LocalDateTime.now().plusHours(DEFAULT_TOKEN_EXPIRY_HOURS);

    user.setPasswordResetToken(token);
    user.setPasswordResetTokenExpiry(expiryDate);
    userRepository.save(user);
    log.info("Password reset token generated for user ID: {}", user.getId());

    // TODO: Email para resetar a senha, token para mudar.
    // Example: emailService.sendPasswordResetEmail(user.getEmail(), token);
    log.info("TODO: Implement email sending for password reset token {} to {}", token, email);
  }

  @Override
  @Transactional
  public void resetPassword(String token, String newPassword) throws Exception {
    log.info("Attempting password reset with token: {}", token);
    if (token == null || token.isEmpty()) {
      log.warn("Password reset failed: Token was null or empty");
      throw new Exception("Token de redefinição inválido ou ausente.");
    }

    User user = userRepository.findByPasswordResetToken(token)
        .orElseThrow(() -> {
          log.warn("Password reset failed: Invalid token provided - {}", token);
          return new Exception("Token de redefinição inválido ou expirado.");
        });

    if (user.getPasswordResetTokenExpiry() == null
        || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
      user.setPasswordResetToken(null);
      user.setPasswordResetTokenExpiry(null);
      userRepository.save(user);
      log.warn("Password reset failed: Token expired for user ID: {}", user.getId());
      throw new Exception("Token de redefinição inválido ou expirado.");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setPasswordResetToken(null);
    user.setPasswordResetTokenExpiry(null);
    userRepository.save(user);
    log.info("Password successfully reset for user ID: {}", user.getId());
    // TODO: Implementar o serviço de email para retornar um aviso de mudança de
    // senha
    // emailService.sendPasswordChangeConfirmationEmail(user.getEmail());
    log.info("TODO: Send password change confirmation email to user ID: {}", user.getId());
  }

  private UserRepresentation mapToRepresentation(User user) {
    return new UserRepresentation(
      user.getId(), 
      user.getFirstName(), 
      user.getLastName(), 
      user.getEmail(), 
      user.getProfileImg(),
      user.getBirthday(),
      user.getCity(),
      user.getState(),
      user.getCountry(),
      user.getCourseName(),
      user.getSemester(),
      user.getUniversity());
  }
}