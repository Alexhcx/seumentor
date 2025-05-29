package com.projetointegrador.seumentor.tutoring.controller;

import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;
import com.projetointegrador.seumentor.tutoring.api.TutoringCommand;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/tutoring")
@RequiredArgsConstructor
@Tag(name = "Mentorias (Tutoring)", description = "Endpoints para agendamento, gerenciamento e avaliação de mentorias")
@SecurityRequirement(name = "bearerAuth")
public class TutoringController {

    private final TutoringCommand tutoringCommandService;
    private final TutoringQuery tutoringQueryService;
    private static final Logger log = LoggerFactory.getLogger(TutoringController.class);

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and (#id == authentication.principal.id or hasAuthority('ADMIN'))")
    @Operation(summary = "Busca mentoria por ID", description = "Retorna os detalhes de uma mentoria específica. Requer que o usuário seja participante ou ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mentoria encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "404", description = "Mentoria não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> getTutoringById(
            @Parameter(description = "ID da mentoria a ser buscada", required = true, in = ParameterIn.PATH) @PathVariable Long id,
            @Parameter(hidden = true) Authentication authentication) {
        log.info("Received request to get Tutoring by ID: {}", id);
        return tutoringQueryService.findTutoringById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> {
                    log.warn("Tutoring not found for ID {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Monitoria não encontrada com ID: " + id);
                });
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()") // General authenticated access might be sufficient, or restrict further
    @Operation(summary = "Lista mentorias com filtros", description = "Retorna uma lista de mentorias, permitindo filtrar por mentor, disciplina e status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mentorias retornada com sucesso", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TutoringRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<TutoringRepresentation>> getAllTutorings(
            @Parameter(description = "ID do Mentor para filtrar (opcional)", required = false, in = ParameterIn.QUERY, example = "10") @RequestParam(required = false) Long mentorId,
            @Parameter(description = "ID da Disciplina para filtrar (opcional)", required = false, in = ParameterIn.QUERY, example = "25") @RequestParam(required = false) Long disciplineId,
            @Parameter(description = "Status da Mentoria para filtrar (opcional)", required = false, in = ParameterIn.QUERY, schema = @Schema(implementation = StatusTutoring.class)) @RequestParam(required = false) StatusTutoring status) {
        log.info("Received request to get Tutorings with filters - MentorId: {}, DisciplineId: {}, Status: {}",
                mentorId, disciplineId, status);
        List<TutoringRepresentation> tutorings = tutoringQueryService.findFilteredTutorings(mentorId, disciplineId,
                status);
        return ResponseEntity.ok(tutorings);
    }

    @GetMapping("/{tutoringId}/ratings")
    @PreAuthorize("hasAuthority('ADMIN') or @tutoringSecurityService.isMentorOfTutoring(authentication, #tutoringId)")
    @Operation(summary = "Lista a avaliação de uma mentoria específica", description = "Retorna a avaliação (se existir) para a mentoria especificada. Requer permissão de ADMIN ou ser o mentor da mentoria.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avaliação(ões) da mentoria retornada(s) com sucesso. A lista conterá 0 ou 1 avaliação.", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TutoringRatingRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria não encontrada", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<TutoringRatingRepresentation>> getTutoringRatings(
            @Parameter(description = "ID da mentoria para buscar a avaliação", required = true) @PathVariable Long tutoringId) {
        log.info("Received request to get ratings for tutoring ID: {} (ADMIN or Mentor)", tutoringId);
        try {
            List<TutoringRatingRepresentation> ratings = tutoringQueryService.findRatingsByTutoringId(tutoringId);
            return ResponseEntity.ok(ratings);
        } catch (TutoringNotFoundException e) {
            log.warn("Get ratings failed: Tutoring not found for ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving ratings for tutoring ID {}: {}", tutoringId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro interno ao buscar avaliações da mentoria.", e);
        }
    }

    @GetMapping("/mentors/{mentorId}/average-rating")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca a média de avaliações de um mentor", description = "Retorna a média das notas de todas as avaliações recebidas por um mentor específico, "
            +
            "juntamente com o número total de avaliações consideradas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Média de avaliações retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MentorAverageRatingRepresentation.class))),
            @ApiResponse(responseCode = "404", description = "Mentor não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado (usuário não autenticado)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<MentorAverageRatingRepresentation> getMentorAverageRating(
            @Parameter(description = "ID do mentor para o qual a média de avaliações será calculada", required = true) @PathVariable Long mentorId) {
        log.info("Received request to get average rating for mentor ID: {}", mentorId);
        try {
            MentorAverageRatingRepresentation avgRating = tutoringQueryService.getMentorAverageRating(mentorId)
                    .orElseThrow(() -> {
                        log.warn("Average rating request failed: Mentor not found for ID {}", mentorId);
                        return new UserNotFoundException("Mentor não encontrado com ID: " + mentorId);
                    });
            return ResponseEntity.ok(avgRating);
        } catch (UserNotFoundException e) {
            log.warn("Get average rating failed: Mentor not found for ID {}: {}", mentorId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving average rating for mentor ID {}: {}", mentorId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro interno ao buscar a média de avaliações do mentor.", e);
        }
    }

    @GetMapping("/by-date")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca mentorias e disponibilidades por data", description = "Retorna uma lista combinada de mentorias concretas e disponibilidades de mentor para uma data específica, usando o TutoringRepresentation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mentorias/disponibilidades retornada com sucesso", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TutoringRepresentation.class)))),
            @ApiResponse(responseCode = "400", description = "Data inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<TutoringRepresentation>> getTutoringsAndAvailabilitiesByDate(
            @Parameter(description = "Data para buscar (formato AAAA-MM-DD)", required = true, in = ParameterIn.QUERY, example = "2025-05-20")
            // Updated example date
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Received request to get Tutorings and Availabilities for date: {}", date);
        try {
            List<TutoringRepresentation> results = tutoringQueryService.findTutoringAndAvailabilityByDate(date);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error retrieving Tutorings/Availabilities for date {}: {}", date, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao buscar mentorias e disponibilidades por data.", e);
        }
    }

    @GetMapping("/users/{userId}/disciplines/{disciplineId}/available-tutorings")
    @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Busca mentorias e disponibilidades para o usuário logado em uma disciplina", description = "Retorna uma lista de mentorias e disponibilidades de OUTROS mentores para o usuário especificado ({userId}), em uma disciplina e data específicas. Exclui mentorias e disponibilidades ofertadas pelo próprio {userId}. Filtra horários que já passaram no dia corrente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de horários disponíveis retornada com sucesso", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TutoringRepresentation.class)))),
            @ApiResponse(responseCode = "400", description = "Data inválida (ex: data no passado)", content = @Content),
    // ... (outras ApiResponses)
    })
    public ResponseEntity<List<TutoringRepresentation>> getAvailableTutoringsForUserAndDiscipline(
            @Parameter(description = "ID do usuário para o qual se busca mentorias/disponibilidades (deve ser o ID do usuário autenticado ou requisição feita por ADMIN)", required = true) @PathVariable Long userId,
            @Parameter(description = "ID da Disciplina para filtrar", required = true) @PathVariable Long disciplineId,
            @Parameter(description = "Data para buscar (formato AAAA-MM-DD)", required = true, in = ParameterIn.QUERY, example = "2025-05-24") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(hidden = true) Authentication authentication) {
        log.info("Received request for available tutorings for user ID: {} in discipline ID: {} on date: {}",
                userId, disciplineId, date);
        if (date.isBefore(LocalDate.now())) {
            log.warn("Failed to get available tutorings for user {}: Date {} is in the past.", userId, date);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data da consulta não pode ser no passado.");
        }

        try {
            List<TutoringRepresentation> results = tutoringQueryService.findAvailableSlotsForUser(date,
                    Optional.of(disciplineId), userId);
            return ResponseEntity.ok(results);
        } catch (UserNotFoundException | DisciplineNotFoundException e) {
            log.warn("Failed to get available tutorings for user {}: {}", userId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving available tutorings for user ID {} on date {}: {}", userId, date,
                    e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar horários disponíveis.",
                    e);
        }
    }

    @PostMapping("/schedule")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Agenda uma nova mentoria", description = "Cria uma solicitação de mentoria entre um mentor e um mentorado para uma disciplina específica. Requer que o solicitante seja o mentorado ou um ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Mentoria agendada com sucesso (status inicial PENDENTE ou AGENDADA, dependendo da lógica)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScheduledTutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: horário inválido, dados faltando)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário (Mentor/Mentorado) ou Disciplina não encontrado(a)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não é o mentorado nem ADMIN)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<ScheduledTutoringRepresentation> scheduleTutoring(
            @RequestBody(description = "Dados para o agendamento da mentoria", required = true, content = @Content(schema = @Schema(implementation = ScheduleTutoringRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody ScheduleTutoringRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        log.info("Received request to schedule tutoring: {}", request);
        try {
            ScheduledTutoringRepresentation response = tutoringCommandService.scheduleTutoring(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserNotFoundException | DisciplineNotFoundException e) {
            log.warn("Scheduling tutoring failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Scheduling tutoring failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Scheduling tutoring failed due to access denied: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error scheduling tutoring: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao agendar monitoria.",
                    e);
        }
    }

    @PostMapping("/{tutoringId}/participants")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adiciona o usuário autenticado como participante a uma mentoria", description = "Inscreve o usuário autenticado como participante em uma mentoria agendada. Requer autenticação. Admins também podem usar este endpoint para adicionar qualquer usuário.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Participante adicionado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: mentoria não está no status correto, mentoria lotada, usuário já participa)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria ou Usuário (do payload) não encontrado(a)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário autenticado não tem permissão para adicionar o userId do payload, ou outras regras de negócio)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> addParticipant(
            @Parameter(description = "ID da mentoria à qual adicionar o participante", required = true, in = ParameterIn.PATH) @PathVariable Long tutoringId,
            @RequestBody(description = "ID do usuário a ser adicionado (deve ser o ID do usuário autenticado, a menos que o requisitante seja ADMIN) e o tópico de interesse", required = true, content = @Content(schema = @Schema(implementation = AddParticipantRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody AddParticipantRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        log.info(
                "Received request to add participant with userId {} (from request body) to tutoring ID: {}. Authenticated user: {}",
                request.userId(), tutoringId, authentication.getName());
        try {
            TutoringRepresentation response = tutoringCommandService.addParticipant(tutoringId, request,
                    authentication);
            return ResponseEntity.ok(response);
        } catch (TutoringNotFoundException | UserNotFoundException e) {
            log.warn("Add participant failed for tutoring ID {} (participant userId in request: {}): {}", tutoringId,
                    request.userId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Add participant failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) { // Será lançada pelo serviço se a autorização falhar
            log.warn("Add participant failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error adding participant (userId in request: {}) to tutoring ID {}: {}", request.userId(),
                    tutoringId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro interno ao adicionar participante.", e);
        }
    }

    @PostMapping("/{tutoringId}/ratings")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Adiciona uma avaliação a uma mentoria concluída", description = "Permite que um participante avalie uma mentoria após sua conclusão. Requer autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Avaliação adicionada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRatingRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: mentoria não está CONCLUIDA, mentoria já avaliada)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria ou Usuário (avaliador) não encontrado(a)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não participou da mentoria)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRatingRepresentation> addTutoringRating(
            @Parameter(description = "ID da mentoria a ser avaliada", required = true, in = ParameterIn.PATH) @PathVariable Long tutoringId,
            @RequestBody(description = "Dados da avaliação (nota e comentário)", required = true, content = @Content(schema = @Schema(implementation = TutoringRatingRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody TutoringRatingRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        log.info("Received request to add rating for tutoring ID: {}", tutoringId);
        try {
            TutoringRatingRepresentation response = tutoringCommandService.addTutoringRating(tutoringId, request,
                    authentication);
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao adicionar avaliação.",
                    e);
        }
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('MENTOR') or hasAuthority('ADMIN')")
    @Operation(summary = "Confirma e atualiza detalhes de uma mentoria agendada", description = "Permite ao mentor confirmar uma mentoria (status AGENDADA) adicionando local/link, número máximo de participantes e se o chat está ativo. Requer autenticação.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mentoria confirmada e atualizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: status não é AGENDADA, dados faltando para tipo online/presencial)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não é o mentor ou admin)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> confirmAndUpdatetutoring(
            @Parameter(description = "ID da mentoria a ser confirmada", required = true, in = ParameterIn.PATH) @PathVariable Long id,
            @RequestBody(description = "Detalhes para confirmação da mentoria", required = true, content = @Content(schema = @Schema(implementation = ConfirmTutoringRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody ConfirmTutoringRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        log.info("Received request to confirm and update tutoring ID: {}", id);
        try {
            TutoringRepresentation response = tutoringCommandService.confirmAndUpdatetutoring(id, request,
                    authentication);
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao confirmar monitoria.",
                    e);
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualiza o status de uma mentoria", description = "Altera o status de uma mentoria (ex: para EM_ANDAMENTO, CONCLUIDA, CANCELADA). Requer autenticação e permissões adequadas (verificado no serviço).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status da mentoria atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: transição de status não permitida)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário sem permissão para esta alteração de status)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> updateTutoringStatus(
            @Parameter(description = "ID da mentoria cujo status será atualizado", required = true, in = ParameterIn.PATH) @PathVariable Long id,
            @RequestBody(description = "Novo status para a mentoria", required = true, content = @Content(schema = @Schema(implementation = UpdateTutoringStatusRequest.class))) @Valid @org.springframework.web.bind.annotation.RequestBody UpdateTutoringStatusRequest request,
            @Parameter(hidden = true) Authentication authentication) {
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro interno ao atualizar status da monitoria.", e);
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
            @Parameter(description = "ID da avaliação a ser excluída", required = true, in = ParameterIn.PATH) @PathVariable Long ratingId) {
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao excluir avaliação.",
                    e);
        }
    }

    @GetMapping("/{id}/mentoring-sessions")
    @PreAuthorize("#id == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Lista as mentorias em que o usuário é o mentor", description = "Retorna uma lista de todas as sessões de mentoria onde o usuário especificado atua como mentor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mentorias (como mentor) retornada com sucesso", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TutoringRepresentation.class)))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<TutoringRepresentation>> getUserMentoringSessions(
            @Parameter(description = "ID do usuário", required = true) @PathVariable Long id) {
        log.info("Controller: Request for mentoring sessions for user ID (as mentor): {}", id);
        try {
            List<TutoringRepresentation> sessions = tutoringQueryService.getUserMentoringSessions(id);
            return ResponseEntity.ok(sessions);
        } catch (UserNotFoundException e) {
            log.warn("Controller: User not found when fetching mentoring sessions for user ID {}: {}", id,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Error fetching mentoring sessions for user ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao buscar sessões de mentoria (como mentor)", e);
        }
    }

    @GetMapping("/{id}/participation-sessions")
    @PreAuthorize("#id == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Lista as mentorias em que o usuário é participante", description = "Retorna uma lista de todas as sessões de mentoria onde o usuário especificado está inscrito como participante (mentorado), com foco nos tópicos e quantidade de participantes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mentorias (como participante) retornada com sucesso", content = @Content(mediaType = "application/json",
                    // Altere o schema para o novo DTO
                    array = @ArraySchema(schema = @Schema(implementation = TutoringParticipationRepresentation.class)))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<TutoringParticipationRepresentation>> getUserParticipationSessions(
            @Parameter(description = "ID do usuário", required = true) @PathVariable Long id) {
        log.info(
                "Controller: Requisição para sessões de participação do usuário ID: {} usando TutoringParticipationRepresentation",
                id);
        try {
            List<TutoringParticipationRepresentation> sessions = tutoringQueryService.getUserParticipationSessions(id);
            return ResponseEntity.ok(sessions);
        } catch (UserNotFoundException e) {
            log.warn("Controller: Usuário não encontrado ao buscar sessões de participação para o ID {}: {}", id,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Erro ao buscar sessões de participação para o usuário ID {}: {}", id, e.getMessage(),
                    e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao buscar sessões de participação em mentoria", e);
        }
    }

    @DeleteMapping("/users/{userId}/tutoring/{tutoringId}/cancel-by-mentor")
    @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Cancela uma mentoria (pelo mentor)", description = "Permite que o mentor (identificado por userId) cancele uma de suas mentorias. "
            +
            "Opcionalmente, pode desativar a disponibilidade correspondente. " +
            "Requer que o usuário autenticado seja o dono do perfil (userId) ou um ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mentoria cancelada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TutoringRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: status não permite cancelamento)", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não autorizado para este user ID ou esta mentoria)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria ou Usuário (do path) não encontrado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<TutoringRepresentation> cancelMentorTutoring(
            @Parameter(description = "ID do usuário mentor", required = true) @PathVariable Long userId,
            @Parameter(description = "ID da mentoria a ser cancelada", required = true) @PathVariable Long tutoringId,
            @Parameter(description = "Se true, desativa também a disponibilidade do mentor que cobria esta mentoria (se encontrada e ativa)", required = false) @RequestParam(required = false, defaultValue = "false") boolean deactivateAvailability,
            @Parameter(hidden = true) Authentication authentication) {
        log.info(
                "Controller: User {} attempting to cancel tutoring ID: {} for mentor user ID: {} with deactivateAvailability: {}",
                authentication.getName(), tutoringId, userId, deactivateAvailability);
        try {
            TutoringRepresentation cancelledTutoring = tutoringCommandService.cancelMentorTutoring(userId, tutoringId,
                    deactivateAvailability, authentication);
            return ResponseEntity.ok(cancelledTutoring);
        } catch (TutoringNotFoundException | UserNotFoundException e) {
            log.warn("Controller: Cancel tutoring failed for tutoring ID {} or user ID {}: {}", tutoringId, userId,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Controller: Cancel tutoring failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Controller: Access denied for cancelling tutoring ID {} for user ID {}: {}", tutoringId, userId,
                    e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Error cancelling tutoring ID {} for user ID {}: {}", tutoringId, userId,
                    e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao cancelar mentoria.", e);
        }
    }

    @DeleteMapping("/{tutoringId}/participants/{userId}")
    @PreAuthorize("#userId == authentication.principal.id or hasAuthority('ADMIN')")
    @Operation(summary = "Remove um participante de uma mentoria", description = "Permite que um mentorado saia de uma mentoria da qual participa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Participante removido com sucesso (mentorado saiu da mentoria)", content = @Content),
            @ApiResponse(responseCode = "400", description = "Operação inválida (ex: mentor tentando sair como participante, não é participante)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Mentoria ou Usuário (participante) não encontrado(a)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<Void> leaveTutoring(
            @Parameter(description = "ID da mentoria da qual o mentorado deseja sair", required = true) @PathVariable Long tutoringId,
            @Parameter(description = "ID do usuário (mentorado) que está saindo", required = true) @PathVariable Long userId,
            @Parameter(hidden = true) Authentication authentication) {

        log.info("Controller: Usuário {} solicitando sair da mentoria ID {} como participante ID {}",
                authentication.getName(), tutoringId, userId);
        try {
            tutoringCommandService.removeParticipant(tutoringId, userId, authentication);
            return ResponseEntity.noContent().build();
        } catch (TutoringNotFoundException | UserNotFoundException e) {
            log.warn("Controller: Falha ao sair da mentoria ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) {
            log.warn("Controller: Falha ao sair da mentoria ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) {
            log.warn("Controller: Acesso negado ao tentar sair da mentoria ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Controller: Erro ao processar saída da mentoria ID {}: {}", tutoringId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro interno ao processar saída da mentoria.", e);
        }
    }
}