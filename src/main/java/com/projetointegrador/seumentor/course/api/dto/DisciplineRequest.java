package com.projetointegrador.seumentor.course.api.dto;

import jakarta.validation.constraints.NotBlank; 
import jakarta.validation.constraints.NotNull;  

public record DisciplineRequest(
    @NotBlank(message = "Nome da disciplina não pode ser vazio")
    String disciplineName,
    String description, 
    @NotNull(message = "ID da área do curso não pode ser nulo")
    Long courseAreaId
) {}
