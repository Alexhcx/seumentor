package com.projetointegrador.seumentor.course.controller;

import com.projetointegrador.seumentor.course.api.dto.DisciplineRepresentation;
import com.projetointegrador.seumentor.course.api.dto.DisciplineRequest;
import com.projetointegrador.seumentor.course.exception.CourseAreaNotFoundException;
import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;
import com.projetointegrador.seumentor.course.service.DisciplineService;
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
@RequestMapping("/api/v1/disciplines")
@RequiredArgsConstructor
@Tag(name = "Disciplinas", description = "Endpoints para gerenciamento de disciplinas")
@SecurityRequirement(name = "bearerAuth")
public class DisciplineController {

    private final DisciplineService disciplineService;
    private static final Logger log = LoggerFactory.getLogger(DisciplineController.class);

    @PostMapping
    @Operation(summary = "Cria uma nova Disciplina", description = "Registra uma nova disciplina associada a uma Área de Curso existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Disciplina criada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DisciplineRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: dados faltando, disciplina já existe na área)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Área de curso (CourseArea) não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<DisciplineRepresentation> createDiscipline(
            @RequestBody(description = "Dados da nova disciplina", required = true,
                    content = @Content(schema = @Schema(implementation = DisciplineRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody DisciplineRequest request) {
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
    @Operation(summary = "Lista todas as Disciplinas", description = "Retorna uma lista de todas as disciplinas cadastradas, opcionalmente filtradas por Área de Curso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de disciplinas retornada com sucesso",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = DisciplineRepresentation.class)))),
            @ApiResponse(responseCode = "404", description = "Área de curso (CourseArea) não encontrada (se o filtro for usado e inválido)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<List<DisciplineRepresentation>> getAllDisciplines(
            @Parameter(description = "ID da Área de Curso para filtrar as disciplinas (opcional)", required = false, example = "5")
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
    @Operation(summary = "Busca Disciplina por ID", description = "Retorna os detalhes de uma disciplina específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disciplina encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DisciplineRepresentation.class))),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<DisciplineRepresentation> getDisciplineById(
            @Parameter(description = "ID da disciplina a ser buscada", required = true)
            @PathVariable Long id) {
        log.info("Received request to get Discipline by ID: {}", id);
        try {
            DisciplineRepresentation discipline = disciplineService.getDisciplineById(id);
            return ResponseEntity.ok(discipline);
        } catch (DisciplineNotFoundException e) {
            log.warn("Discipline not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error retrieving Discipline with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar disciplina por ID", e);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma Disciplina", description = "Atualiza os dados de uma disciplina existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disciplina atualizada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DisciplineRepresentation.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (ex: disciplina já existe na nova área)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Disciplina ou Área de Curso não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<DisciplineRepresentation> updateDiscipline(
            @Parameter(description = "ID da disciplina a ser atualizada", required = true)
            @PathVariable Long id,
            @RequestBody(description = "Novos dados para a disciplina", required = true,
                    content = @Content(schema = @Schema(implementation = DisciplineRequest.class)))
            @Valid @org.springframework.web.bind.annotation.RequestBody DisciplineRequest request) {
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
        } catch (Exception e) { // Outros erros 500
            log.error("Error updating Discipline with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao atualizar disciplina", e);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui uma Disciplina", description = "Exclui uma disciplina se ela não possuir dados associados (disponibilidades, mentorias, etc.).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Disciplina excluída com sucesso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflito - Disciplina possui dados associados", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content)
    })
    public ResponseEntity<Void> deleteDiscipline(
            @Parameter(description = "ID da disciplina a ser excluída", required = true)
            @PathVariable Long id) {
        log.info("Received request to delete Discipline with ID: {}", id);
        try {
            disciplineService.deleteDiscipline(id);
            return ResponseEntity.noContent().build();
        } catch (DisciplineNotFoundException e) {
            log.warn("Delete failed. Discipline not found for ID {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (DataIntegrityViolationException e) { // Erro específico 409
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
