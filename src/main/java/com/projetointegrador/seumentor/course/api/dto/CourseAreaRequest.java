package com.projetointegrador.seumentor.course.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados necessários para criar ou atualizar uma Área de Curso")
public record CourseAreaRequest(
        @NotBlank
        @Schema(description = "Nome do curso (ex: Engenharia, Direito)", example = "Ciência da Computação", requiredMode = Schema.RequiredMode.REQUIRED)
        String course,

        @NotBlank
        @Schema(description = "Nome da área dentro do curso (ex: Cálculo, Programação)", example = "Inteligência Artificial", requiredMode = Schema.RequiredMode.REQUIRED)
        String area
) {}