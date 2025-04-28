package com.projetointegrador.seumentor.tutoring.controller;

import com.projetointegrador.seumentor.tutoring.api.TutoringCommand;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tutoring")
@RequiredArgsConstructor
@Tag(name = "Mentorias (Tutoring)", description = "Endpoints para agendamento, gerenciamento e avaliação de mentorias")
@SecurityRequirement(name = "bearerAuth")
public class TutoringController {

    private final TutoringCommand tutoringCommandService;
    private final TutoringQuery tutoringQueryService;
    private static final Logger log = LoggerFactory.getLogger(TutoringController.class);

    @PostMapping("/schedule")
    @PreAuthorize("#request.menteeId() == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Agenda uma nova mentoria", description = "Cria uma solicitação de mentoria entre um mentor e um mentorado para uma disciplina específica. Requer que o solicitante seja o mentorado ou um ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Mentoria agendada com sucesso (status inicial PENDENTE ou AGENDADA, dependendo da lógica)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: horário inválido, dados faltando)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário (Mentor/Mentorado) ou Disciplina não encontrado(a)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não é o mentorado nem ADMIN)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> scheduleTutoring(
            @RequestBody(description = "Dados para o agendamento da mentoria", required = true,
                    content = @Content(schema = @Schema(implementation = ScheduleTutoringRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody ScheduleTutoringRequest request) {

        log.info("Received request to schedule tutoring: {}", request);
        try {
            TutoringRepresentation response = tutoringCommandService.scheduleTutoring(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserNotFoundException | DisciplineNotFoundException e) {
            log.warn("Scheduling tutoring failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Scheduling tutoring failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
        catch (Exception e) {
            log.error("Error scheduling tutoring: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao agendar monitoria.", e);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or @tutoringSecurityService.canAccessTutoring(authentication, #id)")
    @Operation(summary = "Busca mentoria por ID", description = "Retorna os detalhes de uma mentoria específica. Requer que o usuário seja participante ou ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mentoria encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "404", description = "Mentoria não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> getTutoringById(
            @Parameter(description = "ID da mentoria a ser buscada", required = true, in = ParameterIn.PATH)
            @PathVariable Long id) {
        log.info("Received request to get Tutoring by ID: {}", id);
        return tutoringQueryService.findTutoringById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> {
                    log.warn("Tutoring not found for ID {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitoria não encontrada com ID: " + id);
                });
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista mentorias com filtros", description = "Retorna uma lista de mentorias, permitindo filtrar por mentor, disciplina e status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mentorias retornada com sucesso",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TutoringRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<TutoringRepresentation>> getAllTutorings(
            @Parameter(description = "ID do Mentor para filtrar (opcional)", required = false, in = ParameterIn.QUERY, example = "10")
            @RequestParam(required = false) Long mentorId,
            @Parameter(description = "ID da Disciplina para filtrar (opcional)", required = false, in = ParameterIn.QUERY, example = "25")
            @RequestParam(required = false) Long disciplineId,
            @Parameter(description = "Status da Mentoria para filtrar (opcional)", required = false, in = ParameterIn.QUERY, schema = @Schema(implementation = StatusTutoring.class))
            @RequestParam(required = false) StatusTutoring status
    ) {
        log.info("Received request to get Tutorings with filters - MentorId: {}, DisciplineId: {}, Status: {}",
                mentorId, disciplineId, status);
        List<TutoringRepresentation> tutorings = tutoringQueryService.findFilteredTutorings(mentorId, disciplineId, status);
        return ResponseEntity.ok(tutorings);
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirma e atualiza detalhes de uma mentoria agendada", description = "Permite ao mentor confirmar uma mentoria (status AGENDADA) adicionando local/link, número máximo de participantes e se o chat está ativo. Requer autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mentoria confirmada e atualizada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: status não é AGENDADA, dados faltando para tipo online/presencial)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não é o mentor)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> confirmAndUpdatetutoring(
            @Parameter(description = "ID da mentoria a ser confirmada", required = true, in = ParameterIn.PATH)
            @PathVariable Long id,
            @RequestBody(description = "Detalhes para confirmação da mentoria", required = true,
                    content = @Content(schema = @Schema(implementation = ConfirmTutoringRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody ConfirmTutoringRequest request,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        log.info("Received request to confirm and update tutoring ID: {}", id);
        try {
            TutoringRepresentation response = tutoringCommandService.confirmAndUpdatetutoring(id, request, authentication);
            return ResponseEntity.ok(response);
        } catch (TutoringNotFoundException e) {
            log.warn("Confirm tutoring failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Confirm tutoring failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Confirm tutoring failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error confirming tutoring ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao confirmar monitoria.", e);
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualiza o status de uma mentoria", description = "Altera o status de uma mentoria (ex: para EM_ANDAMENTO, CONCLUIDA, CANCELADA). Requer autenticação e permissões adequadas (geralmente mentor ou ADMIN).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status da mentoria atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: transição de status não permitida)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário sem permissão para alterar status)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> updateTutoringStatus(
            @Parameter(description = "ID da mentoria cujo status será atualizado", required = true, in = ParameterIn.PATH)
            @PathVariable Long id,
            @RequestBody(description = "Novo status para a mentoria", required = true,
                    content = @Content(schema = @Schema(implementation = UpdateTutoringStatusRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody UpdateTutoringStatusRequest request,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        log.info("Received request to update status for tutoring ID: {}", id);
        try {
            TutoringRepresentation response = tutoringCommandService.updateTutoringStatus(id, request, authentication);
            return ResponseEntity.ok(response);
        } catch (TutoringNotFoundException e) {
            log.warn("Update status failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Update status failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Update status failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating status for tutoring ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao atualizar status da monitoria.", e);
        }
    }
    @PostMapping("/{tutoringId}/participants")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adiciona um participante a uma mentoria", description = "Inscreve um usuário como participante em uma mentoria agendada. Requer autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Participante adicionado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: mentoria não está AGENDADA, mentoria lotada, usuário já participa)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria ou Usuário não encontrado(a)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (regra de negócio impede inscrição)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> addParticipant(
            @Parameter(description = "ID da mentoria à qual adicionar o participante", required = true, in = ParameterIn.PATH)
            @PathVariable Long tutoringId,
            @RequestBody(description = "ID do usuário a ser adicionado e o tópico de interesse", required = true,
                    content = @Content(schema = @Schema(implementation = AddParticipantRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody AddParticipantRequest request,
            @Parameter(hidden = true)
            Authentication authentication
    ) {
        log.info("Received request to add participant to tutoring ID: {}", tutoringId);
        try {
            TutoringRepresentation response = tutoringCommandService.addParticipant(tutoringId, request, authentication);
            return ResponseEntity.ok(response);
        } catch (TutoringNotFoundException | UserNotFoundException e) {
            log.warn("Add participant failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Add participant failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Add participant failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error adding participant to tutoring ID {}: {}", tutoringId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao adicionar participante.", e);
        }
    }

    @GetMapping("/ratings")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Lista todas as avaliações de mentorias (ADMIN)", description = "Retorna uma lista de todas as avaliações registradas. Requer permissão de ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de avaliações retornada com sucesso",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TutoringRatingRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não é ADMIN)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<TutoringRatingRepresentation>> getAllTutoringRatings() {
        log.info("Received request to get all tutoring ratings (ADMIN)");
        try {
            List<TutoringRatingRepresentation> ratings = tutoringQueryService.findAllTutoringRatings();
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            log.error("Error retrieving all tutoring ratings: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao buscar avaliações.", e);
        }
    }
    @PostMapping("/{tutoringId}/ratings")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adiciona uma avaliação a uma mentoria concluída", description = "Permite que um participante avalie uma mentoria após sua conclusão. Requer autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Avaliação adicionada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRatingRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: mentoria não está CONCLUIDA, mentoria já avaliada)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria ou Usuário (avaliador) não encontrado(a)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não participou da mentoria)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRatingRepresentation> addTutoringRating(
            @Parameter(description = "ID da mentoria a ser avaliada", required = true, in = ParameterIn.PATH)
            @PathVariable Long tutoringId,
            @RequestBody(description = "Dados da avaliação (nota e comentário)", required = true,
                    content = @Content(schema = @Schema(implementation = TutoringRatingRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody TutoringRatingRequest request,
            @Parameter(hidden = true)
            Authentication authentication) {

        log.info("Received request to add rating for tutoring ID: {}", tutoringId);
        try {
            TutoringRatingRepresentation response = tutoringCommandService.addTutoringRating(tutoringId, request, authentication);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (TutoringNotFoundException | UserNotFoundException e) {
            log.warn("Add rating failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Add rating failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Add rating failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error adding rating for tutoring ID {}: {}", tutoringId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao adicionar avaliação.", e);
        }
    }

    @DeleteMapping("/ratings/{ratingId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Exclui uma avaliação de mentoria (ADMIN)", description = "Remove uma avaliação específica pelo seu ID. Requer permissão de ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Avaliação excluída com sucesso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não é ADMIN)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<Void> deleteTutoringRating(
            @Parameter(description = "ID da avaliação a ser excluída", required = true, in = ParameterIn.PATH)
            @PathVariable Long ratingId) {
        log.info("Received request from ADMIN to delete tutoring rating with ID: {}", ratingId);
        try {
            tutoringCommandService.deleteTutoringRating(ratingId);
            return ResponseEntity.noContent().build();
        } catch (TutoringOperationException e) {
            log.warn("Delete rating failed for ID {}: {}", ratingId, e.getMessage());
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("não encontrada")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("Error deleting tutoring rating with ID {}: {}", ratingId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao excluir avaliação.", e);
        }
    }
}
