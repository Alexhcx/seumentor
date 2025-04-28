package com.projetointegrador.seumentor.course.controller;

import com.projetointegrador.seumentor.course.api.dto.CourseAreaRepresentation;
import com.projetointegrador.seumentor.course.api.dto.CourseAreaRequest;
import com.projetointegrador.seumentor.course.exception.CourseAreaNotFoundException;
import com.projetointegrador.seumentor.course.service.CourseAreaService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course-areas")
@RequiredArgsConstructor
@Tag(name = "Áreas de Curso", description = "Endpoints para gerenciamento de áreas de curso e suas associações")
@SecurityRequirement(name = "bearerAuth")
public class CourseAreaController {

    private final CourseAreaService courseAreaService;
    private static final Logger log = LoggerFactory.getLogger(CourseAreaController.class);

    @PostMapping
    @Operation(summary = "Cria uma nova Área de Curso", description = "Registra uma nova combinação de curso e área.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Área de curso criada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseAreaRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: dados faltando, combinação já existe)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<CourseAreaRepresentation> createCourseArea(
            @RequestBody(description = "Dados da nova área de curso", required = true,
                    content = @Content(schema = @Schema(implementation = CourseAreaRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody CourseAreaRequest request) {
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
    @Operation(summary = "Lista todas as Áreas de Curso", description = "Retorna uma lista de todas as áreas de curso cadastradas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = CourseAreaRepresentation.class)))),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
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
    @Operation(summary = "Busca Área de Curso por ID", description = "Retorna os detalhes de uma área de curso específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Área de curso encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseAreaRepresentation.class))),
            @ApiResponse(responseCode = "404", description = "Área de curso não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<CourseAreaRepresentation> getCourseAreaById(
            @Parameter(description = "ID da área de curso a ser buscada", required = true)
            @PathVariable Long id) {
        log.info("Received request to get CourseArea by ID: {}", id);
        try {
            CourseAreaRepresentation courseArea = courseAreaService.getCourseAreaById(id);
            return ResponseEntity.ok(courseArea);
        } catch (CourseAreaNotFoundException e) {
            log.warn("CourseArea not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving CourseArea with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar área de curso por ID", e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma Área de Curso", description = "Atualiza os dados de uma área de curso existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Área de curso atualizada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CourseAreaRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: combinação já existe)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Área de curso não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<CourseAreaRepresentation> updateCourseArea(
            @Parameter(description = "ID da área de curso a ser atualizada", required = true)
            @PathVariable Long id,
            @RequestBody(description = "Novos dados para a área de curso", required = true,
                    content = @Content(schema = @Schema(implementation = CourseAreaRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody CourseAreaRequest request) {
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
    @Operation(summary = "Exclui uma Área de Curso", description = "Exclui uma área de curso se ela não possuir disciplinas associadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Área de curso excluída com sucesso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Área de curso não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflito - Área de curso possui disciplinas associadas", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<Void> deleteCourseArea(
            @Parameter(description = "ID da área de curso a ser excluída", required = true)
            @PathVariable Long id) {
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