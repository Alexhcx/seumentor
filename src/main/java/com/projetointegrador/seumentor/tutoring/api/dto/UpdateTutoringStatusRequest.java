package com.projetointegrador.seumentor.tutoring.api.dto;

import com.projetointegrador.seumentor.tutoring.model.StatusTutoring; // Importar
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados necessários para atualizar o status de uma mentoria")
public record UpdateTutoringStatusRequest(
        @NotNull
        @Schema(description = "Novo status desejado para a mentoria", example = "CONCLUIDA", requiredMode = Schema.RequiredMode.REQUIRED)
        StatusTutoring status
) {}
