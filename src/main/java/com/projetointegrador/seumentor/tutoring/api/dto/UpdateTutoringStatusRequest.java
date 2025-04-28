package com.projetointegrador.seumentor.tutoring.api.dto;

import com.projetointegrador.seumentor.tutoring.model.StatusTutoring; // Importar
import jakarta.validation.constraints.NotNull;

public record UpdateTutoringStatusRequest(
        @NotNull
        StatusTutoring status
) {}
