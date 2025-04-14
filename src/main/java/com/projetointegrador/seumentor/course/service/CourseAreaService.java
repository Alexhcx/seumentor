package com.projetointegrador.seumentor.course.service;

import com.projetointegrador.seumentor.course.api.dto.CourseAreaRepresentation;
import com.projetointegrador.seumentor.course.api.dto.CourseAreaRequest;
import com.projetointegrador.seumentor.course.exception.CourseAreaNotFoundException;
import com.projetointegrador.seumentor.course.model.CourseArea;
import com.projetointegrador.seumentor.course.repository.CourseAreaRepository;
import com.projetointegrador.seumentor.course.repository.DisciplineRepository;

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
public class CourseAreaService {

    private final CourseAreaRepository courseAreaRepository;
    private final DisciplineRepository disciplineRepository;
    private static final Logger log = LoggerFactory.getLogger(CourseAreaService.class);

    @Transactional
    public CourseAreaRepresentation createCourseArea(CourseAreaRequest request) {
        log.info("Attempting to create CourseArea with name: {} and area: {}", request.course(), request.area());

        if (courseAreaRepository.existsByCourseAndArea(request.course(), request.area())) {
            log.warn("Create failed: CourseArea with name '{}' and area '{}' already exists.", request.course(),
                    request.area());
            throw new IllegalArgumentException("Combinação de curso e área já existe.");
        }

        CourseArea newCourseArea = CourseArea.builder()
                .course(request.course())
                .area(request.area())
                .build();

        CourseArea savedCourseArea = courseAreaRepository.save(newCourseArea);
        log.info("CourseArea created successfully with ID: {}", savedCourseArea.getId());
        return mapToRepresentation(savedCourseArea);
    }

    @Transactional(readOnly = true)
    public List<CourseAreaRepresentation> getAllCourseAreas() {
        log.debug("Attempting to retrieve all CourseAreas");
        List<CourseArea> courseAreas = courseAreaRepository.findAll();
        log.info("Retrieved {} CourseAreas", courseAreas.size());
        return courseAreas.stream()
                .map(this::mapToRepresentation)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseAreaRepresentation getCourseAreaById(Long id) {
        log.debug("Attempting to find CourseArea with ID: {}", id);
        CourseArea courseArea = findCourseAreaByIdOrThrow(id);
        log.debug("CourseArea found with ID: {}", id);
        return mapToRepresentation(courseArea);
    }

    @Transactional
    public CourseAreaRepresentation updateCourseArea(Long id, CourseAreaRequest request) {
        log.info("Attempting to update CourseArea with ID: {}", id);
        CourseArea existingCourseArea = findCourseAreaByIdOrThrow(id);

        boolean nameChanged = !existingCourseArea.getCourse().equals(request.course());
        boolean areaChanged = !existingCourseArea.getArea().equals(request.area());
        if ((nameChanged || areaChanged) &&
                courseAreaRepository.existsByCourseAndArea(request.course(), request.area())) {
            log.warn("Update failed for ID {}: CourseArea with name '{}' and area '{}' already exists.", id,
                    request.course(), request.area());
            throw new IllegalArgumentException("Combinação de curso e área já existe.");
        }

        existingCourseArea.setCourse(request.course());
        existingCourseArea.setArea(request.area());

        CourseArea updatedCourseArea = courseAreaRepository.save(existingCourseArea);
        log.info("CourseArea updated successfully with ID: {}", updatedCourseArea.getId());
        return mapToRepresentation(updatedCourseArea);
    }

    @Transactional
    public void deleteCourseArea(Long id) {
        log.info("Attempting to delete CourseArea with ID: {}", id);
        if (!courseAreaRepository.existsById(id)) {
            log.warn("Delete failed: CourseArea not found with ID: {}", id);
            throw new CourseAreaNotFoundException("Área de curso não encontrada para exclusão com ID: " + id);
        }

        if (disciplineRepository.existsByCourseAreaId(id)) {
            log.warn("Delete failed for ID {}: CourseArea has associated disciplines.", id);
            throw new DataIntegrityViolationException(
                    "Não é possível excluir área de curso pois possui disciplinas associadas.");
        }

        courseAreaRepository.deleteById(id);
        log.info("CourseArea deleted successfully with ID: {}", id);
    }

    private CourseArea findCourseAreaByIdOrThrow(Long id) {
        return courseAreaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("CourseArea not found with ID: {}", id);
                    return new CourseAreaNotFoundException("Área de curso não encontrada com ID: " + id);
                });
    }

    private CourseAreaRepresentation mapToRepresentation(CourseArea courseArea) {
        if (courseArea == null) {
            return null;
        }
        return new CourseAreaRepresentation(
                courseArea.getId(),
                courseArea.getCourse(),
                courseArea.getArea(),
                courseArea.getCreatedAt(),
                courseArea.getUpdatedAt());
    }
}
