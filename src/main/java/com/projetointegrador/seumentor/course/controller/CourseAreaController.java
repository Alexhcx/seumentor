package com.projetointegrador.seumentor.course.controller;

import com.projetointegrador.seumentor.course.api.dto.CourseAreaRepresentation;
import com.projetointegrador.seumentor.course.api.dto.CourseAreaRequest;
import com.projetointegrador.seumentor.course.exception.CourseAreaNotFoundException;
import com.projetointegrador.seumentor.course.service.CourseAreaService;
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
@RequestMapping("/api/v1/course-areas")
@RequiredArgsConstructor
public class CourseAreaController {

    private final CourseAreaService courseAreaService;
    private static final Logger log = LoggerFactory.getLogger(CourseAreaController.class);

    @PostMapping
    public ResponseEntity<CourseAreaRepresentation> createCourseArea(
            @Valid @RequestBody CourseAreaRequest request) {
        log.info("Received request to create CourseArea: {}", request);
        try {
            CourseAreaRepresentation createdCourseArea = courseAreaService.createCourseArea(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCourseArea);
        } catch (IllegalArgumentException e) {
            log.warn("Create CourseArea failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error creating CourseArea: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao criar área de curso", e);
        }
    }

    @GetMapping
    public ResponseEntity<List<CourseAreaRepresentation>> getAllCourseAreas() {
        log.info("Received request to get all CourseAreas");
        try {
            List<CourseAreaRepresentation> courseAreas = courseAreaService.getAllCourseAreas();
            return ResponseEntity.ok(courseAreas);
        } catch (Exception e) {
            log.error("Error retrieving all CourseAreas: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar áreas de curso", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseAreaRepresentation> getCourseAreaById(@PathVariable Long id) {
        log.info("Received request to get CourseArea by ID: {}", id);
        try {
            CourseAreaRepresentation courseArea = courseAreaService.getCourseAreaById(id);
            return ResponseEntity.ok(courseArea);
        } catch (CourseAreaNotFoundException e) {
            log.warn("CourseArea not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving CourseArea with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar área de curso por ID",
                    e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseAreaRepresentation> updateCourseArea(
            @PathVariable Long id,
            @Valid @RequestBody CourseAreaRequest request) {
        log.info("Received request to update CourseArea with ID: {}", id);
        try {
            CourseAreaRepresentation updatedCourseArea = courseAreaService.updateCourseArea(id, request);
            return ResponseEntity.ok(updatedCourseArea);
        } catch (CourseAreaNotFoundException e) {
            log.warn("Update failed. CourseArea not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.warn("Update CourseArea failed for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error updating CourseArea with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao atualizar área de curso", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourseArea(@PathVariable Long id) {
        log.info("Received request to delete CourseArea with ID: {}", id);
        try {
            courseAreaService.deleteCourseArea(id);
            return ResponseEntity.noContent().build();
        } catch (CourseAreaNotFoundException e) {
            log.warn("Delete failed. CourseArea not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (DataIntegrityViolationException e) {
            log.warn("Delete failed for ID {}: CourseArea has associated disciplines.", id);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir área de curso pois possui disciplinas associadas.", e);
        } catch (Exception e) {
            log.error("Error deleting CourseArea with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir área de curso", e);
        }
    }
}