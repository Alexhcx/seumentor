package com.projetointegrador.seumentor.course.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseAreaRequest(
    @NotBlank(message = "Nome do curso não pode ser vazio")
    String course,
    @NotBlank(message = "Área do curso não pode ser vazia")
    String area
) {}