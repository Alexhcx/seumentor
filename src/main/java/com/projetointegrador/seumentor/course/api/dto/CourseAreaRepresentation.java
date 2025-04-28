package com.projetointegrador.seumentor.course.api.dto;

import java.time.LocalDateTime;


public record CourseAreaRepresentation(
    Long id,
    String course,
    String area,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}