package com.projetointegrador.seumentor.course.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados necessários para criar ou atualizar uma Disciplina")
public record DisciplineRequest(
        @NotBlank
        @Schema(description = "Nome da disciplina", example = "Algoritmos e Estruturas de Dados I", requiredMode = Schema.RequiredMode.REQUIRED)
        String disciplineName,

        @Schema(description = "Descrição opcional da disciplina", example = "Estudo de algoritmos fundamentais e estruturas de dados básicas.")
        String description,

        @NotNull
        @Schema(description = "ID da Área de Curso à qual a disciplina pertence", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        Long courseAreaId
) {}