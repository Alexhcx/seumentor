package com.projetointegrador.seumentor.course.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseAreaRequest(
    @NotBlank
    String course,
    @NotBlank
    String area
) {}