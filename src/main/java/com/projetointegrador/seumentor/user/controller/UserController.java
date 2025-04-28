package com.projetointegrador.seumentor.user.controller;

import com.projetointegrador.seumentor.user.api.dtos.UserAvailabilityRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserAvailabilityRequest;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserUpdateRequest;
import com.projetointegrador.seumentor.user.exception.AvailabilityNotFoundException;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.service.UserCommandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de usuários")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserCommandService userCommandService;
  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  @GetMapping
  @Operation(summary = "Lista todos os usuários", description = "Retorna uma lista com a representação de todos os usuários cadastrados.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso",
                  content = { @Content(mediaType = "application/json",
                          array = @ArraySchema(schema = @Schema(implementation = UserRepresentation.class))) }),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<List<UserRepresentation>> getAllUsers() {
    log.info("Received request to get all users");
    List<UserRepresentation> users = userCommandService.getAllUsers();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Busca um usuário pelo ID", description = "Retorna a representação de um usuário específico.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Usuário encontrado",
                  content = { @Content(mediaType = "application/json",
                          schema = @Schema(implementation = UserRepresentation.class)) }),
          @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<UserRepresentation> getUserById(
          @Parameter(description = "ID do usuário a ser buscado", required = true)
          @PathVariable Long id) {
    log.info("Received request to get user by ID: {}", id);
    try {
      UserRepresentation user = userCommandService.getUserById(id);
      return ResponseEntity.ok(user);
    } catch (UserNotFoundException e) {
      log.warn("User not found for ID {}: {}", id, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error fetching user with ID {}: {}", id, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", e);
    }
  }

  @PutMapping("/{id}")
  @Operation(summary = "Atualiza um usuário", description = "Atualiza os dados de um usuário existente baseado no ID fornecido.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
                  content = { @Content(mediaType = "application/json",
                          schema = @Schema(implementation = UserRepresentation.class)) }),
          @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: dados mal formatados)", content = @Content),
          @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<UserRepresentation> updateUser(
          @Parameter(description = "ID do usuário a ser atualizado", required = true)
          @PathVariable Long id,
          @RequestBody(description = "Dados do usuário para atualização", required = true,
                  content = @Content(schema = @Schema(implementation = UserUpdateRequest.class)))
          @org.springframework.web.bind.annotation.RequestBody UserUpdateRequest request) {
    log.info("Received request to update user with ID: {}", id);
    try {
      UserRepresentation updatedUser = userCommandService.updateUser(id, request);
      return ResponseEntity.ok(updatedUser);
    } catch (UserNotFoundException e) {
      log.warn("Update failed. User not found for ID {}: {}", id, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error updating user with ID {}: {}", id, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Erro ao processar a atualização: " + e.getMessage(), e);
    }
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Exclui um usuário", description = "Exclui um usuário permanentemente baseado no ID fornecido.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso", content = @Content),
          @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<Void> deleteUser(
          @Parameter(description = "ID do usuário a ser excluído", required = true)
          @PathVariable Long id) {
    log.info("Received request to delete user with ID: {}", id);
    try {
      userCommandService.deleteUser(id);
      return ResponseEntity.noContent().build();
    } catch (UserNotFoundException e) {
      log.warn("Delete failed. User not found for ID {}: {}", id, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error deleting user with ID {}: {}", id, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir usuário", e);
    }
  }

  @PostMapping("/{userId}/availabilities")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  @Operation(summary = "Adiciona um horário de disponibilidade para um usuário", description = "Registra um novo período em que um usuário (mentor) está disponível para uma disciplina específica. Requer autenticação e autorização (próprio usuário ou ADMIN).")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "201", description = "Disponibilidade criada com sucesso",
                  content = { @Content(mediaType = "application/json",
                          schema = @Schema(implementation = UserAvailabilityRepresentation.class)) }),
          @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: dados inválidos, horário sobreposto)", content = @Content),
          @ApiResponse(responseCode = "404", description = "Usuário ou Disciplina não encontrado(a)", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não permitido ou sem permissão ADMIN)", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<UserAvailabilityRepresentation> addAvailability(
          @Parameter(description = "ID do usuário para o qual adicionar disponibilidade", required = true)
          @PathVariable Long userId,
          @RequestBody(description = "Dados da nova disponibilidade", required = true,
                  content = @Content(schema = @Schema(implementation = UserAvailabilityRequest.class)))
          @Valid @org.springframework.web.bind.annotation.RequestBody UserAvailabilityRequest request) {

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
  @Operation(summary = "Lista as disponibilidades de um usuário", description = "Retorna todos os horários de disponibilidade registrados para um usuário específico. Requer autenticação e autorização.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Disponibilidades listadas com sucesso",
                  content = { @Content(mediaType = "application/json",
                          array = @ArraySchema(schema = @Schema(implementation = UserAvailabilityRepresentation.class))) }),
          @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<List<UserAvailabilityRepresentation>> getUserAvailabilities(
          @Parameter(description = "ID do usuário cujas disponibilidades serão listadas", required = true)
          @PathVariable Long userId) {
    log.info("Request to get availabilities for user ID: {} (Authorized)", userId);

    try {
      List<UserAvailabilityRepresentation> availabilities = userCommandService.getUserAvailabilities(userId);
      return ResponseEntity.ok(availabilities);
    } catch (UserNotFoundException e) {
      log.warn("Get availabilities failed: User not found for ID {}: {}", userId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error retrieving availabilities for user {}: {}", userId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar disponibilidades", e);
    }
  }

  @DeleteMapping("/{userId}/availabilities/{availabilityId}")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  @Operation(summary = "Exclui uma disponibilidade específica", description = "Remove um horário de disponibilidade pelo seu ID. Requer autenticação e autorização.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "Disponibilidade excluída com sucesso", content = @Content),
          @ApiResponse(responseCode = "404", description = "Disponibilidade não encontrada", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<Void> deleteAvailability(
          @Parameter(description = "ID do usuário proprietário da disponibilidade", required = true)
          @PathVariable Long userId,
          @Parameter(description = "ID da disponibilidade a ser excluída", required = true)
          @PathVariable Long availabilityId) {
    log.info("Request to delete availability with ID: {} for user ID: {} (Authorized based on userId)", availabilityId, userId);
    try {
      userCommandService.deleteAvailability(availabilityId);
      return ResponseEntity.noContent().build();
    } catch (AvailabilityNotFoundException e) {
      log.warn("Delete availability failed: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Error deleting availability with ID {}: {}", availabilityId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir disponibilidade", e);
    }
  }

}