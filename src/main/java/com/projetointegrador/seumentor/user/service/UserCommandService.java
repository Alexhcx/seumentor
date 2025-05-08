// main/java/com/projetointegrador/seumentor/user/service/UserCommandService.java
package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.common.util.CPFUtils;
import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;
import com.projetointegrador.seumentor.user.api.UserQuery; // Apenas a interface
import com.projetointegrador.seumentor.user.api.dtos.*;
import com.projetointegrador.seumentor.user.exception.FavoriteDisciplineNotFoundException;
import com.projetointegrador.seumentor.user.model.UserFavoriteDisciplines;
import com.projetointegrador.seumentor.user.repository.UserFavoriteDisciplinesRepository;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projetointegrador.seumentor.course.api.DisciplineQuery;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.user.api.UserCommand;
import com.projetointegrador.seumentor.user.api.events.PasswordResetRequestedEvent;
import com.projetointegrador.seumentor.user.api.events.UserRegisteredEvent;
import com.projetointegrador.seumentor.user.exception.AvailabilityNotFoundException;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import com.projetointegrador.seumentor.user.model.Role;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.model.MentorAvailability;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;

@Service
@RequiredArgsConstructor
public class UserCommandService implements UserCommand {

  private final UserRepository userRepository;
  private final MentorAvailabilityRepository mentorAvailabilityRepository;
  private final UserFavoriteDisciplinesRepository userFavoriteDisciplinesRepository;

  private final DisciplineQuery disciplineQuery;
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
      log.error("Command: Failed to publish UserRegisteredEvent for user ID {}: {}", savedUser.getId(), e.getMessage(), e);
    }

    return userQuery.findById(savedUser.getId())
            .orElseThrow(() -> new IllegalStateException("Falha ao buscar representação do usuário recém-criado: " + savedUser.getId()));
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
            .orElseThrow(() -> new IllegalStateException("Falha ao buscar representação do usuário recém-atualizado: " + updatedUser.getId()));
  }

  @Transactional
  public void deleteUser(Long userId) {
    log.info("Command: Attempting to delete user with ID: {}", userId);
    if (!userRepository.existsById(userId)) {
      log.warn("Command: Delete failed: User not found with ID: {}", userId);
      throw new UserNotFoundException("Usuário não encontrado para exclusão com ID: " + userId);
    }
    // Deleta
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

    // Opcional: Publicar um evento se necessário (ex: notificar o usuário sobre a troca de senha)
    // UserPasswordChangedEvent event = new UserPasswordChangedEvent(user.getId(), user.getEmail());
    // eventPublisher.publishEvent(event);
  }
  @Transactional
  public UserAvailabilityRepresentation addAvailability(Long userId, UserAvailabilityRequest request) {
    log.info("Command: Attempting to add availability for user ID: {} with discipline ID: {}", userId, request.disciplineId());

    if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
      throw new IllegalArgumentException("O horário de início deve ser anterior ao horário de fim.");
    }

    User user = userRepository.findById(userId)
            .orElseThrow(() -> {
              log.warn("Command: Add availability failed: User not found with ID {}", userId);
              return new UserNotFoundException("Usuário não encontrado com ID: " + userId);
            });

    if (user.getRole() == Role.USER) {
      log.info("Command: User ID {} is adding first availability. Changing role from USER to MENTOR.", userId);
      user.setRole(Role.MENTOR);
    }

    Discipline disciplineRef = disciplineQuery.findBasicInfoById(request.disciplineId())
            .map(info -> disciplineQuery.getReferenceById(info.id()))
            .orElseThrow(() -> {
              log.warn("Command: Add availability failed: Discipline not found with ID {}", request.disciplineId());
              return new DisciplineNotFoundException("Disciplina não encontrada com ID: " + request.disciplineId());
            });

    MentorAvailability newAvailability = MentorAvailability.builder()
            .user(user)
            .discipline(disciplineRef)
            .dayOfWeek(request.dayOfWeek())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .tutoringClassType(request.tutoringClassType())
            .isAvailable(false)
            .build();

    MentorAvailability savedAvailability = mentorAvailabilityRepository.save(newAvailability);
    log.info("Command: Availability entity added successfully with ID: {} for user ID: {}", savedAvailability.getId(), userId);

    return userQuery.findAvailabilityRepresentationById(savedAvailability.getId())
            .orElseThrow(() -> new IllegalStateException("Falha ao buscar representação da disponibilidade recém-criada: " + savedAvailability.getId()));
  }

  @Transactional
  public List<UserAvailabilityRepresentation> updateAvailabilityStatus(Long userId, Long availabilityId, UpdateAvailabilityStatusRequest request) {
    log.info("Command: Attempting to update availability status for ID: {} (User: {}) to {}", availabilityId, userId, request.isAvailable());

    MentorAvailability targetAvailability = mentorAvailabilityRepository.findById(availabilityId)
            .orElseThrow(() -> {
              log.warn("Command: Update status failed: Availability not found with ID: {}", availabilityId);
              return new AvailabilityNotFoundException("Horário de disponibilidade não encontrado com ID: " + availabilityId);
            });

    if (!targetAvailability.getUser().getId().equals(userId)) {
      log.warn("Command: Update status failed: Availability ID {} does not belong to user ID {}", availabilityId, userId);
      throw new AccessDeniedException("Usuário não autorizado a modificar esta disponibilidade.");
    }

    boolean newStatus = request.isAvailable();
    List<MentorAvailability> availabilitiesToSave = new ArrayList<>();

    if (newStatus) {
      log.debug("Command: Activating availability ID {}. Checking for conflicts for user ID {} on day {}",
              availabilityId, userId, targetAvailability.getDayOfWeek());

      List<MentorAvailability> sameDayAvailabilities = mentorAvailabilityRepository.findByUserIdAndDayOfWeek(
              userId, targetAvailability.getDayOfWeek()
      );

      List<MentorAvailability> modifiedConflicts = new ArrayList<>();

      for (MentorAvailability otherAvailability : sameDayAvailabilities) {
        if (otherAvailability.getId().equals(availabilityId)) {
          continue;
        }

        boolean overlaps = doesOverlap(targetAvailability, otherAvailability);

        if (overlaps && otherAvailability.getIsAvailable()) {
          log.debug("Command: Availability ID {} conflicts with target ID {}. Deactivating.", otherAvailability.getId(), availabilityId);
          otherAvailability.setIsAvailable(false);
          modifiedConflicts.add(otherAvailability);
        }
      }

      if (!modifiedConflicts.isEmpty()) {
        availabilitiesToSave.addAll(modifiedConflicts);
      }

      targetAvailability.setIsAvailable(true);
      availabilitiesToSave.add(targetAvailability);

    } else {
      log.debug("Command: Deactivating availability ID {}", availabilityId);
      if (targetAvailability.getIsAvailable()) {
        targetAvailability.setIsAvailable(false);
        availabilitiesToSave.add(targetAvailability);
      }
    }

    if (!availabilitiesToSave.isEmpty()) {
      mentorAvailabilityRepository.saveAll(availabilitiesToSave);
      log.info("Command: Saved {} availability status changes for user ID {}", availabilitiesToSave.size(), userId);
    }

    return userQuery.findAvailabilitiesRepresentationByUserId(userId);
  }


  private boolean doesOverlap(MentorAvailability target, MentorAvailability other) {
    LocalTime targetStart = target.getStartTime();
    LocalTime targetEnd = target.getEndTime();
    LocalTime otherStart = other.getStartTime();
    LocalTime otherEnd = other.getEndTime();

    return targetStart.isBefore(otherEnd) && targetEnd.isAfter(otherStart);
  }

  @Transactional
  public void deleteAvailability(Long availabilityId) {
    log.info("Command: Attempting to delete availability with ID: {}", availabilityId);
    if (!mentorAvailabilityRepository.existsById(availabilityId)) {
      log.warn("Command: Delete availability failed: Availability not found with ID: {}", availabilityId);
      throw new AvailabilityNotFoundException("Horário de disponibilidade não encontrado com ID: " + availabilityId);
    }
    // TODO: Adicionar verificação de segurança (somente o dono ou ADMIN podem excluir)
    // Ex: buscar a entidade, verificar o user.id e comparar com o usuário autenticado/verificar role ADMIN

    mentorAvailabilityRepository.deleteById(availabilityId);
    log.info("Command: Availability deleted successfully with ID: {}", availabilityId);
  }

  @Transactional
  public void addFavoriteDiscipline(Long userId, Long disciplineId) {
    log.info("Command: Attempting to add favorite discipline ID {} for user ID {}", disciplineId, userId);

    if (userFavoriteDisciplinesRepository.existsByUserIdAndDisciplineId(userId, disciplineId)) {
      log.warn("Command: User ID {} already has discipline ID {} as a favorite. Skipping.", userId, disciplineId);
      return;
    }

    User user = userQuery.getUserReferenceById(userId);
    Discipline discipline = disciplineQuery.findBasicInfoById(disciplineId)
            .map(info -> disciplineQuery.getReferenceById(info.id()))
            .orElseThrow(() -> {
              log.warn("Command: Add favorite discipline failed: Discipline not found with ID {}", disciplineId);
              return new DisciplineNotFoundException("Disciplina não encontrada com ID: " + disciplineId);
            });

    UserFavoriteDisciplines favorite = UserFavoriteDisciplines.builder()
            .user(user)
            .discipline(discipline)
            .build();

    userFavoriteDisciplinesRepository.save(favorite);
    log.info("Command: Successfully added discipline ID {} as favorite for user ID {}", disciplineId, userId);
  }

  @Transactional
  public void deleteFavoriteDiscipline(Long userId, Long disciplineId) {
    log.info("Command: Attempting to delete favorite discipline ID {} for user ID {}", disciplineId, userId);

    UserFavoriteDisciplines favorite = userFavoriteDisciplinesRepository.findByUserIdAndDisciplineId(userId, disciplineId)
            .orElseThrow(() -> {
              log.warn("Command: Delete favorite discipline failed: Favorite link not found for user ID {} and discipline ID {}", userId, disciplineId);
              return new FavoriteDisciplineNotFoundException(
                      "Disciplina favorita não encontrada para o usuário ID " + userId + " e disciplina ID " + disciplineId);
            });
    // TODO: Adicionar verificação de segurança (somente o dono ou ADMIN)

    userFavoriteDisciplinesRepository.delete(favorite);
    log.info("Command: Successfully deleted favorite discipline ID {} for user ID {}", disciplineId, userId);
  }
}