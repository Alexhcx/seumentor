package com.projetointegrador.seumentor.course.controller;

import com.projetointegrador.seumentor.course.api.dto.DisciplineRepresentation;
import com.projetointegrador.seumentor.course.api.dto.DisciplineRequest;
import com.projetointegrador.seumentor.course.exception.CourseAreaNotFoundException;
import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;
import com.projetointegrador.seumentor.course.service.DisciplineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disciplines")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService disciplineService;
    private static final Logger log = LoggerFactory.getLogger(DisciplineController.class);

    @PostMapping
    public ResponseEntity<DisciplineRepresentation> createDiscipline(
            @Valid @RequestBody DisciplineRequest request) {
        log.info("Received request to create Discipline: {}", request);
        try {
            DisciplineRepresentation createdDiscipline = disciplineService.createDiscipline(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDiscipline);
        } catch (CourseAreaNotFoundException e) {
            log.warn("Create Discipline failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Create Discipline failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error creating Discipline: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao criar disciplina", e);
        }
    }

    @GetMapping
    public ResponseEntity<List<DisciplineRepresentation>> getAllDisciplines(
            @RequestParam(name = "courseAreaId", required = false) Long courseAreaId) {
        log.info("Received request to get all Disciplines"
                + (courseAreaId != null ? " for CourseArea ID: " + courseAreaId : ""));
        try {
            List<DisciplineRepresentation> disciplines = disciplineService.getAllDisciplines(courseAreaId);
            return ResponseEntity.ok(disciplines);
        } catch (CourseAreaNotFoundException e) {
            log.warn("Get Disciplines failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving Disciplines: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar disciplinas", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplineRepresentation> getDisciplineById(@PathVariable Long id) {
        log.info("Received request to get Discipline by ID: {}", id);
        try {
            DisciplineRepresentation discipline = disciplineService.getDisciplineById(id);
            return ResponseEntity.ok(discipline);
        } catch (DisciplineNotFoundException e) {
            log.warn("Discipline not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e); // Lança 404
        } catch (Exception e) {
            log.error("Error retrieving Discipline with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar disciplina por ID", e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisciplineRepresentation> updateDiscipline(
            @PathVariable Long id,
            @Valid @RequestBody DisciplineRequest request) {
        log.info("Received request to update Discipline with ID: {}", id);
        try {
            DisciplineRepresentation updatedDiscipline = disciplineService.updateDiscipline(id, request);
            return ResponseEntity.ok(updatedDiscipline);
        } catch (DisciplineNotFoundException | CourseAreaNotFoundException e) {
            log.warn("Update failed for Discipline ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Update Discipline failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating Discipline with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao atualizar disciplina", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiscipline(@PathVariable Long id) {
        log.info("Received request to delete Discipline with ID: {}", id);
        try {
            disciplineService.deleteDiscipline(id);
            return ResponseEntity.noContent().build();
        } catch (DisciplineNotFoundException e) {
            log.warn("Delete failed. Discipline not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (DataIntegrityViolationException e) {
            log.warn("Delete failed for ID {}: Discipline has associated data (availabilities, tutorings, etc.).", id);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir disciplina pois possui dados associados (disponibilidades, mentorias, etc.).",
                    e);
        } catch (Exception e) {
            log.error("Error deleting Discipline with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir disciplina", e);
        }
    }
}
