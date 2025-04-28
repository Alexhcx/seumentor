package com.projetointegrador.seumentor.tutoring.api.dto;

import jakarta.validation.constraints.Min; // Para validar maxParticipants
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record ConfirmTutoringRequest(

        @Length(max = 255)
        String local,

        @Length(max = 512)
        String linkVideo,

        @Min(value = 1)
        Integer maxParticipants,

        @NotNull
        Boolean isChatEnable
) {}
