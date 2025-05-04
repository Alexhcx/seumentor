package com.projetointegrador.seumentor.user.controller;

import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;

import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.*;
import com.projetointegrador.seumentor.user.exception.AvailabilityNotFoundException;
import com.projetointegrador.seumentor.user.exception.FavoriteDisciplineNotFoundException;
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
  private final UserQuery userQuery;
  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  @GetMapping
  @PreAuthorize("hasAuthority('ADMIN')")
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
    log.info("Controller: Received request to get all users");
    // Chama o método de UserQuery
    List<UserRepresentation> users = userQuery.findAllUserRepresentations();
    return ResponseEntity.ok(users);
  }

  @GetMapping("/{id}")
  @PreAuthorize("#id == authentication.principal.id or hasAuthority('ADMIN')")
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
    log.info("Controller: Received request to get user by ID: {}", id);
    UserRepresentation user = userQuery.findById(id)
            .orElseThrow(() -> {
              log.warn("Controller: User not found for ID {}", id);
              return new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com ID: " + id);
            });
    return ResponseEntity.ok(user);
  }

  @GetMapping("/{userId}/mentor_availability")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  @Operation(summary = "Lista as disponibilidades de um usuário", description = "Retorna todos os horários de disponibilidade registrados para um usuário específico.")
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
    log.info("Controller: Request to get availabilities for user ID: {} (Authorized)", userId);
    try {
      List<UserAvailabilityRepresentation> availabilities = userQuery.findAvailabilitiesRepresentationByUserId(userId);
      return ResponseEntity.ok(availabilities);
    } catch (UserNotFoundException e) {
      log.warn("Controller: Get availabilities failed: User not found for ID {}: {}", userId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Controller: Error retrieving availabilities for user {}: {}", userId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar disponibilidades", e);
    }
  }

  @GetMapping("/mentors/{id}/profile")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Busca o perfil detalhado de um mentor pelo ID", description = "Retorna ID, nome, sobrenome, curso, e a lista de disciplinas com seus respectivos horários de disponibilidade do mentor.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Perfil do mentor encontrado",
                  content = { @Content(mediaType = "application/json",
                          schema = @Schema(implementation = MentorProfileRepresentation.class)) }),
          @ApiResponse(responseCode = "404", description = "Mentor não encontrado", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<MentorProfileRepresentation> getMentorProfileById(
          @Parameter(description = "ID do mentor a ser buscado", required = true)
          @PathVariable Long id) {
    log.info("Controller: Received request to get mentor profile by ID: {}", id);
    MentorProfileRepresentation mentorProfile = userQuery.findMentorProfileById(id)
            .orElseThrow(() -> {
              log.warn("Controller: Mentor profile not found for ID {}", id);
              return new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de mentor não encontrado com ID: " + id);
            });
    return ResponseEntity.ok(mentorProfile);
  }

  @GetMapping("/mentors/profiles")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Lista os perfis detalhados de todos os mentores", description = "Retorna uma lista com ID, nome, sobrenome, curso, e a lista de disciplinas com seus respectivos horários de disponibilidade para cada mentor.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Perfis dos mentores listados com sucesso",
                  content = { @Content(mediaType = "application/json",
                          array = @ArraySchema(schema = @Schema(implementation = MentorProfileRepresentation.class))) }),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<List<MentorProfileRepresentation>> getAllMentorProfiles() {
    log.info("Controller: Received request to get all mentor profiles");
    List<MentorProfileRepresentation> mentorProfiles = userQuery.findAllMentorProfiles();
    return ResponseEntity.ok(mentorProfiles);
  }

  @PutMapping("/{id}")
  @PreAuthorize("#id == authentication.principal.id or hasAuthority('ADMIN')")
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
          @Valid @org.springframework.web.bind.annotation.RequestBody UserUpdateRequest request) {
    log.info("Controller: Received request to update user with ID: {}", id);
    try {
      UserRepresentation updatedUser = userCommandService.updateUser(id, request);
      return ResponseEntity.ok(updatedUser);
    } catch (UserNotFoundException e) {
      log.warn("Controller: Update failed. User not found for ID {}: {}", id, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Controller: Error updating user with ID {}: {}", id, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao processar a atualização.", e);
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("#id == authentication.principal.id or hasAuthority('ADMIN')")
  @Operation(summary = "Exclui um usuário e sua conta", description = "Exclui um usuário e sua conta permanentemente baseado no ID fornecido.")
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
    log.info("Controller: Received request to delete user with ID: {}", id);
    try {
      // Chama o command service
      userCommandService.deleteUser(id);
      return ResponseEntity.noContent().build();
    } catch (UserNotFoundException e) {
      log.warn("Controller: Delete failed. User not found for ID {}: {}", id, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Controller: Error deleting user with ID {}: {}", id, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir usuário", e);
    }
  }

  @PostMapping("/{userId}/favorite-disciplines/{disciplineId}")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  @Operation(summary = "Adiciona uma disciplina como favorita para o usuário", description = "Marca uma disciplina específica como favorita.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "Disciplina adicionada aos favoritos com sucesso", content = @Content),
          @ApiResponse(responseCode = "404", description = "Usuário ou Disciplina não encontrado(a)", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não permitido)", content = @Content),
          @ApiResponse(responseCode = "409", description = "Conflito (disciplina já é favorita)", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<Void> addFavoriteDiscipline(
          @Parameter(description = "ID do usuário", required = true) @PathVariable Long userId,
          @Parameter(description = "ID da disciplina a ser favoritada", required = true) @PathVariable Long disciplineId) {
    log.info("Controller: Request to add favorite discipline ID {} for user ID {} (Authorized)", disciplineId, userId);
    try {
      userCommandService.addFavoriteDiscipline(userId, disciplineId);
      return ResponseEntity.noContent().build();
    } catch (UserNotFoundException | DisciplineNotFoundException e) {
      log.warn("Controller: Add favorite discipline failed for user {}: {}", userId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Controller: Error adding favorite discipline for user {}: {}", userId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao adicionar disciplina favorita", e);
    }
  }

  @DeleteMapping("/{userId}/favorite-disciplines/{disciplineId}")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  @Operation(summary = "Remove uma disciplina dos favoritos do usuário", description = "Desmarca uma disciplina específica como favorita.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "204", description = "Disciplina removida dos favoritos com sucesso", content = @Content),
          @ApiResponse(responseCode = "404", description = "Usuário, Disciplina ou o Vínculo Favorito não encontrado", content = @Content),
          @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
          @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não permitido)", content = @Content),
          @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
  })
  public ResponseEntity<Void> deleteFavoriteDiscipline(
          @Parameter(description = "ID do usuário", required = true) @PathVariable Long userId,
          @Parameter(description = "ID da disciplina a ser removida dos favoritos", required = true) @PathVariable Long disciplineId) {
    log.info("Controller: Request to delete favorite discipline ID {} for user ID {} (Authorized)", disciplineId, userId);
    try {
      userCommandService.deleteFavoriteDiscipline(userId, disciplineId);
      return ResponseEntity.noContent().build();
    } catch (UserNotFoundException | DisciplineNotFoundException | FavoriteDisciplineNotFoundException e) {
      log.warn("Controller: Delete favorite discipline failed for user {}: {}", userId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Controller: Error deleting favorite discipline for user {}: {}", userId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao remover disciplina favorita", e);
    }
  }

  @PostMapping("/{userId}/mentor")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  @Operation(summary = "Se torna mentor de uma disciplina e adiciona um horário de disponibilidade", description = "Registra um novo período em que um usuário (mentor) está disponível.")
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
          @Parameter(description = "ID do usuário", required = true) @PathVariable Long userId,
          @RequestBody(description = "Dados da nova disponibilidade", required = true,
                  content = @Content(schema = @Schema(implementation = UserAvailabilityRequest.class)))
          @Valid @org.springframework.web.bind.annotation.RequestBody UserAvailabilityRequest request) {
    log.info("Controller: Request to add availability for user ID: {} (Authorized)", userId);
    try {
      // Chama o command service
      UserAvailabilityRepresentation createdAvailability = userCommandService.addAvailability(userId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdAvailability);
    } catch (UserNotFoundException | DisciplineNotFoundException e) {
      log.warn("Controller: Add availability failed for user {}: {}", userId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    } catch (IllegalArgumentException e) { // Captura erro de horário inválido
      log.warn("Controller: Add availability failed for user {}: {}", userId, e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    } catch (Exception e) {
      log.error("Controller: Error adding availability for user {}: {}", userId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao adicionar disponibilidade", e);
    }
  }


  @DeleteMapping("/{userId}/availabilities/{availabilityId}")
  @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
  @Operation(summary = "Exclui uma disponibilidade específica", description = "Remove um horário de disponibilidade.")
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
    log.info("Controller: Request to delete availability with ID: {} for user ID: {} (Authorized)", availabilityId, userId);
    try {
      userCommandService.deleteAvailability(availabilityId);
      return ResponseEntity.noContent().build();
    } catch (AvailabilityNotFoundException e) {
      log.warn("Controller: Delete availability failed: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
    }
    // TODO: Capturar exceção de Acesso Negado (403) se a lógica for implementada no service
    // catch (AccessDeniedException e) {
    //    log.warn("Controller: Access denied deleting availability {}: {}", availabilityId, e.getMessage());
    //    throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
    // }
    catch (Exception e) {
      log.error("Controller: Error deleting availability with ID {}: {}", availabilityId, e.getMessage(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir disponibilidade", e);
    }
  }
}