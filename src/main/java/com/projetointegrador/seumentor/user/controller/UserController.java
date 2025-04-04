package com.projetointegrador.seumentor.user.controller;

import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserUpdateRequest; 
import com.projetointegrador.seumentor.user.exception.UserNotFoundException; 
import com.projetointegrador.seumentor.user.service.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
  public ResponseEntity<UserRepresentation> getUserById(@PathVariable Integer id) {
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
  public ResponseEntity<UserRepresentation> updateUser(@PathVariable Integer id,
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
  public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
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

}