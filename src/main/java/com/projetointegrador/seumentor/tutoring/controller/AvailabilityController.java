package com.projetointegrador.seumentor.tutoring.controller;

import com.projetointegrador.seumentor.tutoring.api.TutoringCommand;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.UpdateAvailabilityStatusRequest;
import com.projetointegrador.seumentor.tutoring.api.dto.UserAvailabilityRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.UserAvailabilityRequest; // Certifique-se que este é o DTO correto para adicionar
import com.projetointegrador.seumentor.tutoring.exception.AvailabilityNotFoundException;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException; // Pode ser necessário para tratar exceções do TutoringCommand

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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tutoring/mentors/{mentorId}/availabilities") // Novo caminho base proposto
@RequiredArgsConstructor
@Tag(name = "Disponibilidades de Mentoria", description = "Endpoints para gerenciamento de disponibilidades dos mentores")
@SecurityRequirement(name = "bearerAuth")
public class AvailabilityController {

    private final TutoringCommand tutoringCommandService;
    private final TutoringQuery tutoringQueryService;
    private static final Logger log = LoggerFactory.getLogger(AvailabilityController.class);

    @GetMapping
    @PreAuthorize("#mentorId == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Lista as disponibilidades de um mentor", description = "Retorna todos os horários de disponibilidade registrados para um mentor específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disponibilidades listadas com sucesso",
                    content = {@Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserAvailabilityRepresentation.class)))}),
            @ApiResponse(responseCode = "404", description = "Mentor não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<UserAvailabilityRepresentation>> getMentorAvailabilities(
            @Parameter(description = "ID do mentor cujas disponibilidades serão listadas", required = true)
            @PathVariable Long mentorId) {
        log.info("Controller: Request to get availabilities for mentor ID: {} (Authorized)", mentorId);
        try {
            // Nota: O UserQuery.findAvailabilitiesRepresentationByUserId foi movido/adaptado para TutoringQuery
            List<UserAvailabilityRepresentation> availabilities = tutoringQueryService.findMentorAvailabilitiesByMentorId(mentorId);
            return ResponseEntity.ok(availabilities);
        } catch (UserNotFoundException e) {
            log.warn("Controller: Get availabilities failed: Mentor not found for ID {}: {}", mentorId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Error retrieving availabilities for mentor {}: {}", mentorId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar disponibilidades", e);
        }
    }

    @PostMapping
    @PreAuthorize("#mentorId == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Adiciona uma nova disponibilidade para o mentor", description = "Registra um novo período em que um mentor está disponível. Pode promover o usuário a MENTOR se for a primeira disponibilidade.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Disponibilidade criada com sucesso",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserAvailabilityRepresentation.class))}),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: dados inválidos, horário sobreposto)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentor ou Disciplina não encontrado(a)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<UserAvailabilityRepresentation> addMentorAvailability(
            @Parameter(description = "ID do mentor", required = true) @PathVariable Long mentorId,
            @RequestBody(description = "Dados da nova disponibilidade", required = true,
                    content = @Content(schema = @Schema(implementation = UserAvailabilityRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody UserAvailabilityRequest request) {
        log.info("Controller: Request to add availability for mentor ID: {} (Authorized)", mentorId);
        try {
            UserAvailabilityRepresentation createdAvailability = tutoringCommandService.addMentorAvailability(mentorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAvailability);
        } catch (UserNotFoundException | com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException e) { // Ajustar para a exceção correta de disciplina
            log.warn("Controller: Add availability failed for mentor {}: {}", mentorId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException | IllegalArgumentException e) { // IllegalArgumentException para conflitos, etc.
            log.warn("Controller: Add availability failed for mentor {}: {}", mentorId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Error adding availability for mentor {}: {}", mentorId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao adicionar disponibilidade", e);
        }
    }

    @PatchMapping("/{availabilityId}/status")
    @PreAuthorize("#mentorId == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Atualiza o status de uma disponibilidade específica do mentor", description = "Ativa ou desativa um horário de disponibilidade. Se ativado, desativa automaticamente outros horários conflitantes do mesmo mentor no mesmo dia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status da disponibilidade atualizado com sucesso. Retorna a lista completa e atualizada de disponibilidades do mentor.",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = UserAvailabilityRepresentation.class)))),
            @ApiResponse(responseCode = "404", description = "Disponibilidade não encontrada ou não pertence ao mentor", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<UserAvailabilityRepresentation>> updateMentorAvailabilityStatus(
            @Parameter(description = "ID do mentor proprietário da disponibilidade", required = true)
            @PathVariable Long mentorId,
            @Parameter(description = "ID da disponibilidade a ter o status atualizado", required = true)
            @PathVariable Long availabilityId,
            @RequestBody(description = "Novo status de disponibilidade", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateAvailabilityStatusRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateAvailabilityStatusRequest request) {

        log.info("Controller: Request to update status for availability ID: {} for mentor ID: {} to {} (Authorized)", availabilityId, mentorId, request.isAvailable());
        try {
            List<UserAvailabilityRepresentation> updatedAvailabilities = tutoringCommandService.updateMentorAvailabilityStatus(mentorId, availabilityId, request);
            return ResponseEntity.ok(updatedAvailabilities);
        } catch (UserNotFoundException | AvailabilityNotFoundException e) {
            log.warn("Controller: Update availability status failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Controller: Access denied updating availability status for availability {}: {}", availabilityId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Controller: Operation failed updating availability status for availability {}: {}", availabilityId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        catch (Exception e) {
            log.error("Controller: Error updating availability status for availability {}: {}", availabilityId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao atualizar status da disponibilidade", e);
        }
    }

    @DeleteMapping("/{availabilityId}")
    @PreAuthorize("#mentorId == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Exclui uma disponibilidade específica do mentor", description = "Remove um horário de disponibilidade.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Disponibilidade excluída com sucesso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Disponibilidade não encontrada ou não pertence ao mentor", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<Void> deleteMentorAvailability(
            @Parameter(description = "ID do mentor proprietário da disponibilidade", required = true)
            @PathVariable Long mentorId,
            @Parameter(description = "ID da disponibilidade a ser excluída", required = true)
            @PathVariable Long availabilityId,
            @Parameter(hidden = true) Authentication authentication) { // Adicionado para verificações no serviço se necessário
        log.info("Controller: Request to delete availability with ID: {} for mentor ID: {} (Authorized)", availabilityId, mentorId);
        try {
            tutoringCommandService.deleteMentorAvailability(mentorId, availabilityId); // Passando authentication
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException | AvailabilityNotFoundException e) {
            log.warn("Controller: Delete availability failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Controller: Access denied deleting availability {}: {}", availabilityId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Controller: Operation failed deleting availability {}: {}", availabilityId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); // Ou CONFLICT dependendo da natureza
        }
        catch (Exception e) {
            log.error("Controller: Error deleting availability with ID {}: {}", availabilityId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir disponibilidade", e);
        }
    }
}