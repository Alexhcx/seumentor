package com.projetointegrador.seumentor.course.api.dto;

import java.time.LocalDateTime;

public record DisciplineRepresentation(
    Long id,
    String disciplineName,
    String description,
    Long courseAreaId, 
    String courseAreaName, 
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
