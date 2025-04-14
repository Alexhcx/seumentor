package com.projetointegrador.seumentor.user.controller;

import com.projetointegrador.seumentor.user.api.dtos.UserAvailabilityRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserAvailabilityRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserUpdateRequest;
import com.projetointegrador.seumentor.user.exception.AvailabilityNotFoundException;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.service.UserCommandService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserCommandService userCommandService;
  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  @GetMapping
  public ResponseEntity<List<UserRepresentation>> getAllUsers() {
    log.info("Received request to get all users");
    List<UserRepresentation> users = userCommandService.getAllUsers();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserRepresentation> getUserById(@PathVariable Long id) {
    log.info("Received request to get user by ID: {}", id);
    try {
      UserRepresentation user = userCommandService.getUserById(id);
      return ResponseEntity.ok(user);
    } catch (UserNotFoundException e) {
      log.warn("User not found for ID {}: {}", id, e.getMessage());
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error fetching user with ID {}: {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserRepresentation> updateUser(@PathVariable Long id,
      @RequestBody UserUpdateRequest request) {
    log.info("Received request to update user with ID: {}", id);
    try {
      UserRepresentation updatedUser = userCommandService.updateUser(id, request);
      return ResponseEntity.ok(updatedUser);
    } catch (UserNotFoundException e) {
      log.warn("Update failed. User not found for ID {}: {}", id, e.getMessage());
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error updating user with ID {}: {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    log.info("Received request to delete user with ID: {}", id);
    try {
      userCommandService.deleteUser(id);
      return ResponseEntity.noContent().build();
    } catch (UserNotFoundException e) {
      log.warn("Delete failed. User not found for ID {}: {}", id, e.getMessage());
      return ResponseEntity.notFound().build();
    } catch (Exception e) {
      log.error("Error deleting user with ID {}: {}", id, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PostMapping("/{userId}/availabilities")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  public ResponseEntity<UserAvailabilityRepresentation> addAvailability(
      @PathVariable Long userId,
      @Valid @RequestBody UserAvailabilityRequest request) {

    log.info("Request to add availability for user ID: {} (Authorized)", userId);

    try {
      UserAvailabilityRepresentation createdAvailability = userCommandService.addAvailability(userId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdAvailability);
    } catch (UserNotFoundException | EntityNotFoundException e) {
      log.warn("Add availability failed for user {}: {}", userId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (IllegalArgumentException e) {
      log.warn("Add availability failed for user {}: {}", userId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error adding availability for user {}: {}", userId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao adicionar disponibilidade", e);
    }
  }

  @GetMapping("/{userId}/availabilities")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  public ResponseEntity<List<UserAvailabilityRepresentation>> getUserAvailabilities(@PathVariable Long userId) {
    log.info("Request to get availabilities for user ID: {} (Authorized)", userId);

    try {
      List<UserAvailabilityRepresentation> availabilities = userCommandService.getUserAvailabilities(userId);
      return ResponseEntity.ok(availabilities); 
    } catch (UserNotFoundException e) {
      log.warn("Get availabilities failed: User not found for ID {}: {}", userId, e.getMessage());
      return ResponseEntity.notFound().build(); 
    } catch (Exception e) {
      log.error("Error retrieving availabilities for user {}: {}", userId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); 
    }
  }

  @DeleteMapping("/{userId}/availabilities/{availabilityId}")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  public ResponseEntity<Void> deleteAvailability(@PathVariable Long userId, @PathVariable Long availabilityId) {
      log.info("Request to delete availability with ID: {} for user ID: {} (Authorized based on userId)", availabilityId, userId);
      try {
          userCommandService.deleteAvailability(availabilityId);
          return ResponseEntity.noContent().build(); 
      } catch (AvailabilityNotFoundException e) {
          log.warn("Delete availability failed: {}", e.getMessage());
          return ResponseEntity.notFound().build(); 
      } catch (Exception e) {
          log.error("Error deleting availability with ID {}: {}", availabilityId, e.getMessage(), e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }
  }

}