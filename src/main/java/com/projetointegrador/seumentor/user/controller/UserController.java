package com.projetointegrador.seumentor.user.controller;

import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.*;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Endpoints para gerenciamento de dados de usuários e autenticação")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQuery userQuery;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Lista todos os usuários", description = "Retorna uma lista com a representação de todos os usuários cadastrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserRepresentation.class))) }),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<UserRepresentation>> getAllUsers() {
        log.info("Controller: Received request to get all users");
        List<UserRepresentation> users = userQuery.findAllUserRepresentations();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Busca um usuário pelo ID", description = "Retorna a representação de um usuário específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário encontrado", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = UserRepresentation.class)) }),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<UserRepresentation> getUserById(
            @Parameter(description = "ID do usuário a ser buscado", required = true) @PathVariable Long id) {
        log.info("Controller: Received request to get user by ID: {}", id);
        UserRepresentation user = userQuery.findById(id)
                .orElseThrow(() -> {
                    log.warn("Controller: User not found for ID {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com ID: " + id);
                });
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("#id == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Altera a senha do usuário", description = "Permite que o usuário autenticado altere sua própria senha, ou um ADMIN altere a senha de qualquer usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(type = "string", example = "Senha alterada com sucesso."))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<?> changePassword(
            @Parameter(description = "ID do usuário cuja senha será alterada", required = true) @PathVariable Long id,
            @RequestBody(description = "Senha antiga e nova senha", required = true, content = @Content(schema = @Schema(implementation = ChangePasswordRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody ChangePasswordRequest request) {
        log.info("Controller: Received request to change password for user ID: {}", id);
        try {
            userCommandService.changeUserPassword(id, request);
            return ResponseEntity.ok("Senha alterada com sucesso.");
        } catch (UserNotFoundException e) {
            log.warn("Controller: Change password failed. User not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (BadCredentialsException | IllegalArgumentException e) {
            log.warn("Controller: Change password failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) { 
            log.warn("Controller: Access denied changing password for user ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Error changing password for user ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao processar a alteração de senha.", e);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Atualiza um usuário", description = "Atualiza os dados de um usuário existente baseado no ID fornecido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = UserRepresentation.class)) }),
            @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<UserRepresentation> updateUser(
            @Parameter(description = "ID do usuário a ser atualizado", required = true) @PathVariable Long id,
            @RequestBody(description = "Dados do usuário para atualização", required = true, content = @Content(schema = @Schema(implementation = UserUpdateRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody UserUpdateRequest request) {
        log.info("Controller: Received request to update user with ID: {}", id);
        try {
            UserRepresentation updatedUser = userCommandService.updateUser(id, request);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            log.warn("Controller: Update failed. User not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Error updating user with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao processar a atualização do usuário.", e);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Exclui um usuário", description = "Exclui um usuário permanentemente baseado no ID fornecido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Usuário excluído com sucesso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID do usuário a ser excluído", required = true) @PathVariable Long id) {
        log.info("Controller: Received request to delete user with ID: {}", id);
        try {
            userCommandService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException e) {
            log.warn("Controller: Delete failed. User not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Error deleting user with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir usuário.", e);
        }
    }
}