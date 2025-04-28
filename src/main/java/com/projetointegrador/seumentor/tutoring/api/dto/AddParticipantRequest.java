package com.projetointegrador.seumentor.tutoring.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddParticipantRequest(
        @NotNull
        Long userId,

        @NotBlank
        String topic
) {}
