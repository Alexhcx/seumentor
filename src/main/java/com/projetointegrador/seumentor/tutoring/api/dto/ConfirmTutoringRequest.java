package com.projetointegrador.seumentor.tutoring.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min; // Para validar maxParticipants
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@Schema(description = "Dados necessários para o mentor confirmar e detalhar uma mentoria agendada")
public record ConfirmTutoringRequest(

        @Length(max = 255)
        @Schema(description = "Local da mentoria (obrigatório se ClassType for PRESENCIAL)", example = "Sala B203, Bloco B")
        String local,

        @Length(max = 512)
        @Schema(description = "Link para a sala virtual da mentoria (obrigatório se ClassType for ONLINE)", example = "https://meet.example.com/abc-def-ghi")
        String linkVideo,

        @Min(value = 1)
        @Schema(description = "Número máximo de participantes permitidos na mentoria (incluindo o primeiro mentorado)", example = "5")
        Integer maxParticipants,

        @NotNull
        @Schema(description = "Indica se o chat estará habilitado durante a mentoria", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean isChatEnable
) {}
