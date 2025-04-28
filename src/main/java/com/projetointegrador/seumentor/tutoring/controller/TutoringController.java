package com.projetointegrador.seumentor.tutoring.controller;

import com.projetointegrador.seumentor.tutoring.api.TutoringCommand;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;

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
public class TutoringController {

    private final TutoringCommand tutoringCommandService;
    private final TutoringQuery tutoringQueryService;
    private static final Logger log = LoggerFactory.getLogger(TutoringController.class);

    @PostMapping("/schedule")
    @PreAuthorize("#request.menteeId() == authentication.principal.id or hasAuthority('ADMIN')")
    public ResponseEntity<TutoringRepresentation> scheduleTutoring(
            @Valid @RequestBody ScheduleTutoringRequest request) {

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
            // Evitar expor detalhes internos no erro genérico
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao agendar monitoria.", e);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or @tutoringSecurityService.canAccessTutoring(authentication, #id)")
    public ResponseEntity<TutoringRepresentation> getTutoringById(@PathVariable Long id) {
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
    public ResponseEntity<List<TutoringRepresentation>> getAllTutorings(
            @RequestParam(required = false) Long mentorId,
            @RequestParam(required = false) Long disciplineId,
            @RequestParam(required = false) StatusTutoring status
    ) {
        log.info("Received request to get Tutorings with filters - MentorId: {}, DisciplineId: {}, Status: {}",
                mentorId, disciplineId, status);

        List<TutoringRepresentation> tutorings = tutoringQueryService.findFilteredTutorings(mentorId, disciplineId, status);
        return ResponseEntity.ok(tutorings);
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TutoringRepresentation> confirmAndUpdatetutoring(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmTutoringRequest request,
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
    public ResponseEntity<TutoringRepresentation> updateTutoringStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTutoringStatusRequest request,
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
    public ResponseEntity<TutoringRepresentation> addParticipant(
            @PathVariable Long tutoringId,
            @Valid @RequestBody AddParticipantRequest request,
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
    public ResponseEntity<TutoringRatingRepresentation> addTutoringRating(
            @PathVariable Long tutoringId,
            @Valid @RequestBody TutoringRatingRequest request,
            Authentication authentication) {

        log.info("Received request to add rating for tutoring ID: {}", tutoringId);
        try {
            TutoringRatingRepresentation response = tutoringCommandService.addTutoringRating(tutoringId, request, authentication);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (TutoringNotFoundException | UserNotFoundException e) {
            log.warn("Add rating failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (TutoringOperationException e) { // Catches invalid status, already rated, etc.
            log.warn("Add rating failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (AccessDeniedException e) { // Catches if user is not a participant
            log.warn("Add rating failed for tutoring ID {}: {}", tutoringId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error adding rating for tutoring ID {}: {}", tutoringId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao adicionar avaliação.", e);
        }
    }

    @DeleteMapping("/ratings/{ratingId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteTutoringRating(@PathVariable Long ratingId) {
        log.info("Received request from ADMIN to delete tutoring rating with ID: {}", ratingId);
        try {
            tutoringCommandService.deleteTutoringRating(ratingId);
            return ResponseEntity.noContent().build();
        } catch (TutoringOperationException e) {
            log.warn("Delete rating failed for ID {}: {}", ratingId, e.getMessage());
            if (e.getMessage().contains("não encontrada")) {
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
