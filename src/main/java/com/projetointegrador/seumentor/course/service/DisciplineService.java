package com.projetointegrador.seumentor.course.service;

import com.projetointegrador.seumentor.course.api.dto.DisciplineRepresentation;
import com.projetointegrador.seumentor.course.api.dto.DisciplineRequest;
import com.projetointegrador.seumentor.course.exception.CourseAreaNotFoundException;
import com.projetointegrador.seumentor.course.exception.DisciplineNotFoundException;
import com.projetointegrador.seumentor.course.model.CourseArea;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.course.repository.CourseAreaRepository;
import com.projetointegrador.seumentor.course.repository.DisciplineRepository;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.user.api.UserAvailabilityQuery;
import com.projetointegrador.seumentor.user.api.UserPreferenceQuery;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final CourseAreaRepository courseAreaRepository;
    private final TutoringQuery tutoringQuery;
    private final UserAvailabilityQuery userAvailabilityQuery;
    private final UserPreferenceQuery userPreferenceQuery;

    private static final Logger log = LoggerFactory.getLogger(DisciplineService.class);

    @Transactional
    public DisciplineRepresentation createDiscipline(DisciplineRequest request) {
        log.info("Attempting to create Discipline with name: {} for CourseArea ID: {}",
                request.disciplineName(), request.courseAreaId());

        CourseArea courseArea = courseAreaRepository.findById(request.courseAreaId())
                .orElseThrow(() -> {
                    log.warn("Create Discipline failed: CourseArea not found with ID: {}", request.courseAreaId());
                    return new CourseAreaNotFoundException(
                            "Área de curso não encontrada com ID: " + request.courseAreaId());
                });

        if (disciplineRepository.existsByDisciplineNameAndCourseAreaId(request.disciplineName(),
                request.courseAreaId())) {
            log.warn("Create failed: Discipline '{}' already exists in CourseArea ID {}.", request.disciplineName(),
                    request.courseAreaId());
            throw new IllegalArgumentException("Disciplina já existe nesta área de curso.");
        }

        Discipline newDiscipline = Discipline.builder()
                .disciplineName(request.disciplineName())
                .description(request.description())
                .courseArea(courseArea)
                .build();

        Discipline savedDiscipline = disciplineRepository.save(newDiscipline);
        log.info("Discipline created successfully with ID: {}", savedDiscipline.getId());
        return mapToRepresentation(savedDiscipline);
    }

    @Transactional(readOnly = true)
    public List<DisciplineRepresentation> getAllDisciplines(Long courseAreaId) {
        log.debug("Attempting to retrieve all Disciplines"
                + (courseAreaId != null ? " for CourseArea ID: " + courseAreaId : ""));
        List<Discipline> disciplines;
        if (courseAreaId != null) {
            if (!courseAreaRepository.existsById(courseAreaId)) {
                log.warn("Cannot get disciplines: CourseArea not found with ID: {}", courseAreaId);
                throw new CourseAreaNotFoundException("Área de curso não encontrada com ID: " + courseAreaId);
            }
            disciplines = disciplineRepository.findByCourseAreaId(courseAreaId);
            log.info("Retrieved {} Disciplines for CourseArea ID: {}", disciplines.size(), courseAreaId);
        } else {
            disciplines = disciplineRepository.findAll();
            log.info("Retrieved {} Disciplines", disciplines.size());
        }
        return disciplines.stream()
                .map(this::mapToRepresentation)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DisciplineRepresentation getDisciplineById(Long id) {
        log.debug("Attempting to find Discipline with ID: {}", id);
        Discipline discipline = findDisciplineByIdOrThrow(id);
        log.debug("Discipline found with ID: {}", id);
        return mapToRepresentation(discipline);
    }

    @Transactional
    public DisciplineRepresentation updateDiscipline(Long id, DisciplineRequest request) {
        log.info("Attempting to update Discipline with ID: {}", id);
        Discipline existingDiscipline = findDisciplineByIdOrThrow(id);

        CourseArea courseArea = existingDiscipline.getCourseArea();
        if (!courseArea.getId().equals(request.courseAreaId())) {
            log.debug("CourseArea ID changed for Discipline {}. Fetching new CourseArea ID: {}", id,
                    request.courseAreaId());
            courseArea = courseAreaRepository.findById(request.courseAreaId())
                    .orElseThrow(() -> {
                        log.warn("Update Discipline failed: New CourseArea not found with ID: {}",
                                request.courseAreaId());
                        return new CourseAreaNotFoundException(
                                "Nova área de curso não encontrada com ID: " + request.courseAreaId());
                    });
        }

        boolean nameChanged = !existingDiscipline.getDisciplineName().equals(request.disciplineName());
        boolean courseAreaChanged = !existingDiscipline.getCourseArea().getId().equals(request.courseAreaId());
        if ((nameChanged || courseAreaChanged) &&
                disciplineRepository.existsByDisciplineNameAndCourseAreaId(request.disciplineName(),
                        request.courseAreaId())) {
            log.warn("Update failed for ID {}: Discipline '{}' already exists in CourseArea ID {}.", id,
                    request.disciplineName(), request.courseAreaId());
            throw new IllegalArgumentException("Disciplina já existe nesta área de curso.");
        }

        existingDiscipline.setDisciplineName(request.disciplineName());
        existingDiscipline.setDescription(request.description());
        existingDiscipline.setCourseArea(courseArea);

        Discipline updatedDiscipline = disciplineRepository.save(existingDiscipline);
        log.info("Discipline updated successfully with ID: {}", updatedDiscipline.getId());
        return mapToRepresentation(updatedDiscipline);
    }

    @Transactional
    public void deleteDiscipline(Long id) {
        log.info("Attempting to delete Discipline with ID: {}", id);
        if (!disciplineRepository.existsById(id)) {
            log.warn("Delete failed: Discipline not found with ID: {}", id);
            throw new DisciplineNotFoundException("Disciplina não encontrada para exclusão com ID: " + id);
        }

        if (userAvailabilityQuery.existsAvailabilityForDiscipline(id)) {
            log.warn("Delete failed for ID {}: Discipline has associated UserAvailability.", id);
            throw new DataIntegrityViolationException(
                    "Não é possível excluir disciplina pois possui disponibilidades de usuários associadas.");
        }
        if (tutoringQuery.existsTutoringForDiscipline(id)) {
            log.warn("Delete failed for ID {}: Discipline has associated Tutoring sessions.", id);
            throw new DataIntegrityViolationException(
                    "Não é possível excluir disciplina pois possui mentorias associadas.");
        }
        if (userPreferenceQuery.existsPreferenceForDiscipline(id)) {
            log.warn("Delete failed for ID {}: Discipline has associated UserDisciplinePreference.", id);
            throw new DataIntegrityViolationException(
                    "Não é possível excluir disciplina pois possui preferências de usuários associadas.");
        }
        disciplineRepository.deleteById(id);
        log.info("Discipline deleted successfully with ID: {}", id);
    }

    private Discipline findDisciplineByIdOrThrow(Long id) {
        return disciplineRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Discipline not found with ID: {}", id);
                    return new DisciplineNotFoundException("Disciplina não encontrada com ID: " + id);
                });
    }
    //TODO: Mover DisciplineRepresentation mapToRepresentation para o disciplinequeryadapter

    private DisciplineRepresentation mapToRepresentation(Discipline discipline) {
        if (discipline == null) {
            return null;
        }
        Long courseAreaId = null;
        String courseAreaName = null;
        if (discipline.getCourseArea() != null) {
            courseAreaId = discipline.getCourseArea().getId();
            courseAreaName = discipline.getCourseArea().getCourse() + " - " + discipline.getCourseArea().getArea();
        }

        return new DisciplineRepresentation(
                discipline.getId(),
                discipline.getDisciplineName(),
                discipline.getDescription(),
                courseAreaId,
                courseAreaName,
                discipline.getCreatedAt(),
                discipline.getUpdatedAt());
    }
}
