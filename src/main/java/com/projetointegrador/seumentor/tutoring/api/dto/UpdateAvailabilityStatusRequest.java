package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para atualizar o status de disponibilidade de um horário")
public record UpdateAvailabilityStatusRequest(
        @NotNull
        @Schema(description = "Novo status de disponibilidade (true para disponível, false para indisponível)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean isAvailable
) {}
