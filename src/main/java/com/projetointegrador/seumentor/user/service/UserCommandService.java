package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.user.api.dtos.*;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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

import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;

@Service
@RequiredArgsConstructor
public class UserCommandService implements UserCommand {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher eventPublisher;
  private final MentorAvailabilityRepository mentorAvailabilityRepository;
  private final DisciplineQuery disciplineQuery;

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
    newUser.setCpf(request.cpf());
    newUser.setPhone(request.phone());
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

  @Transactional
  public UserRepresentation getUserById(Long userId) {
    log.debug("Attempting to find user with ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("User not found with ID: {}", userId);
          return new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        });
    log.debug("User found with ID: {}", userId);
    return mapToRepresentation(user);
  }

  @Transactional
  public List<UserRepresentation> getAllUsers() {
    log.debug("Attempting to retrieve all users");
    List<User> users = userRepository.findAll();
    log.info("Retrieved {} users", users.size());
    return users.stream()
        .map(this::mapToRepresentation)
        .collect(Collectors.toList());
  }

  @Transactional
  public UserRepresentation updateUser(Long userId, UserUpdateRequest request) {
    log.info("Attempting to update user with ID: {}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("Update failed: User not found with ID: {}", userId);
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
    log.info("User updated successfully with ID: {}", updatedUser.getId());
    return mapToRepresentation(updatedUser);
  }

  @Transactional
  public void deleteUser(Long userId) {
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
          log.warn("Password reset requested for non-existent or existing email: {}", email);
          return new Exception("Operação de reset solicitada.");
        });

    String token = UUID.randomUUID().toString();
    LocalDateTime expiryDate = LocalDateTime.now().plusHours(DEFAULT_TOKEN_EXPIRY_HOURS);

    user.setPasswordResetToken(token);
    user.setPasswordResetTokenExpiry(expiryDate);
    userRepository.save(user);
    log.info("Password reset token generated for user ID: {}", user.getId());

    try {
      PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(
          user.getEmail(),
          user.getFirstName(),
          token);
      eventPublisher.publishEvent(event);
      log.info("PasswordResetRequestedEvent published for email: {}", email);
    } catch (Exception e) {
      log.error("Failed to publish PasswordResetRequestedEvent for email {}: {}", email, e.getMessage(), e);
    }
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
  }

  @Transactional
  public UserAvailabilityRepresentation addAvailability(Long userId, UserAvailabilityRequest request) {
    log.info("Attempting to add availability for user ID: {} with discipline ID: {}", userId, request.disciplineId());

    // Validações de horário, etc.
    if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
      throw new IllegalArgumentException("O horário de início deve ser anterior ao horário de fim.");
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com ID: " + userId));

    if (!disciplineQuery.existsById(request.disciplineId())) {
      log.warn("Add availability failed for user {}: Discipline not found with ID: {}", userId, request.disciplineId());
      throw new EntityNotFoundException("Disciplina não encontrada com ID: " + request.disciplineId());
    }

    Discipline disciplineRef = disciplineQuery.getReferenceById(request.disciplineId());

    MentorAvailability newAvailability = MentorAvailability.builder()
        .user(user)
        .discipline(disciplineRef)
        .dayOfWeek(request.dayOfWeek())
        .startTime(request.startTime())
        .endTime(request.endTime())
        .build();

    MentorAvailability savedAvailability = mentorAvailabilityRepository.save(newAvailability);
    log.info("Availability added successfully with ID: {} for user ID: {}", savedAvailability.getId(), userId);

    return mapToAvailabilityRepresentation(savedAvailability);
  }

  @Transactional
  public void deleteAvailability(Long availabilityId) {
    log.info("Attempting to delete availability with ID: {}", availabilityId);

    if (!mentorAvailabilityRepository.existsById(availabilityId)) {
      log.warn("Delete availability failed: Availability not found with ID: {}", availabilityId);
      throw new AvailabilityNotFoundException("Horário de disponibilidade não encontrado com ID: " + availabilityId);
    }

    // TODO: Verificação de segurança (dono da disponibilidade)

    mentorAvailabilityRepository.deleteById(availabilityId);
    log.info("Availability deleted successfully with ID: {}", availabilityId);
  }

  @Transactional(readOnly = true)
  public List<UserAvailabilityRepresentation> getUserAvailabilities(Long userId) {
    log.debug("Attempting to retrieve availabilities for user ID: {}", userId);

    if (!userRepository.existsById(userId)) {
      log.warn("Cannot retrieve availabilities: User not found with ID: {}", userId);
      throw new UserNotFoundException("Usuário não encontrado com ID: " + userId);
    }

    List<MentorAvailability> availabilities = mentorAvailabilityRepository.findByUserId(userId);
    log.debug("Found {} availabilities for user ID: {}", availabilities.size(), userId);

    return availabilities.stream()
        .map(this::mapToAvailabilityRepresentation)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public MentorProfileRepresentation findMentorProfileById(Long mentorId) {
    log.debug("Attempting to find mentor profile for ID: {}", mentorId);
    User user = userRepository.findById(mentorId)
            .orElseThrow(() -> {
              log.warn("Mentor profile not found: User not found with ID: {}", mentorId);
              return new UserNotFoundException("Mentor não encontrado com ID: " + mentorId);
            });

    List<MentorAvailability> userAvailabilities = mentorAvailabilityRepository.findByUserId(mentorId);
    log.debug("Found {} availabilities for mentor ID: {}", userAvailabilities.size(), mentorId);

    Map<String, List<AvailabilitySlot>> availabilitiesByDiscipline = userAvailabilities.stream()
            .filter(avail -> avail.getDiscipline() != null)
            .collect(Collectors.groupingBy(
                    avail -> avail.getDiscipline().getDisciplineName(),
                    Collectors.mapping(
                            avail -> new AvailabilitySlot(
                                    avail.getDayOfWeek(),
                                    avail.getStartTime(),
                                    avail.getEndTime()
                            ),
                            Collectors.toList()
                    )
            ));

    List<MentorDisciplineAvailabilityRepresentation> disciplineAvailabilities = availabilitiesByDiscipline.entrySet().stream()
            .map(entry -> new MentorDisciplineAvailabilityRepresentation(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    log.info("Successfully mapped mentor profile for ID: {}", mentorId);
    return new MentorProfileRepresentation(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getCourseName(),
            disciplineAvailabilities
    );
  }

  @Transactional(readOnly = true)
  public List<MentorProfileRepresentation> findAllMentorProfiles() {
    log.debug("Attempting to find all mentor profiles.");

    List<MentorAvailability> allAvailabilities = mentorAvailabilityRepository.findAll();
    log.debug("Fetched {} total availabilities.", allAvailabilities.size());

    Map<Long, List<MentorAvailability>> availabilitiesByUser = allAvailabilities.stream()
            .filter(avail -> avail.getUser() != null)
            .collect(Collectors.groupingBy(avail -> avail.getUser().getId()));
    log.debug("Grouped availabilities for {} unique mentors.", availabilitiesByUser.size());

    List<Long> mentorIds = new ArrayList<>(availabilitiesByUser.keySet());

    List<User> mentors = userRepository.findAllById(mentorIds);
    log.debug("Fetched details for {} mentors.", mentors.size());

    List<MentorProfileRepresentation> mentorProfiles = mentors.stream().map(mentor -> {
      List<MentorAvailability> mentorAvailabilities = availabilitiesByUser.getOrDefault(mentor.getId(), Collections.emptyList());

      Map<String, List<AvailabilitySlot>> availabilitiesByDiscipline = mentorAvailabilities.stream()
              .filter(avail -> avail.getDiscipline() != null)
              .collect(Collectors.groupingBy(
                      avail -> avail.getDiscipline().getDisciplineName(),
                      Collectors.mapping(
                              avail -> new AvailabilitySlot(
                                      avail.getDayOfWeek(),
                                      avail.getStartTime(),
                                      avail.getEndTime()
                              ),
                              Collectors.toList()
                      )
              ));

      List<MentorDisciplineAvailabilityRepresentation> disciplineAvailabilities = availabilitiesByDiscipline.entrySet().stream()
              .map(entry -> new MentorDisciplineAvailabilityRepresentation(entry.getKey(), entry.getValue()))
              .collect(Collectors.toList());

      return new MentorProfileRepresentation(
              mentor.getId(),
              mentor.getFirstName(),
              mentor.getLastName(),
              mentor.getCourseName(),
              disciplineAvailabilities
      );
    }).collect(Collectors.toList());

    log.info("Successfully mapped {} mentor profiles.", mentorProfiles.size());
    return mentorProfiles;
  }

 //TODO: Mover para userQueryAdapter
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
        user.getUniversity());
  }

  //TODO: Mover para userQueryAdapter
  private UserAvailabilityRepresentation mapToAvailabilityRepresentation(MentorAvailability availability) {
    if (availability == null) {
      return null;
    }

    Discipline discipline = availability.getDiscipline();

    SimpleDisciplineRepresentation userApiDisciplineRep = new SimpleDisciplineRepresentation(
        discipline != null ? discipline.getId() : null,
        discipline != null ? discipline.getDisciplineName() : "[Disciplina inválida]");

    return new UserAvailabilityRepresentation(
        availability.getId(),
        userApiDisciplineRep,
        availability.getDayOfWeek(),
        availability.getStartTime(),
        availability.getEndTime());
  }
}