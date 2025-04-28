package com.projetointegrador.seumentor.course.api.dto;

import jakarta.validation.constraints.NotBlank; 
import jakarta.validation.constraints.NotNull;  

public record DisciplineRequest(
    @NotBlank
    String disciplineName,
    String description, 
    @NotNull
    Long courseAreaId
) {}
