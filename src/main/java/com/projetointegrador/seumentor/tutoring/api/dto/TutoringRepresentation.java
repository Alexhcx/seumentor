package com.projetointegrador.seumentor.tutoring.api.dto;

import com.projetointegrador.seumentor.tutoring.model.ClassType;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "Representação completa dos dados de uma mentoria")
public record TutoringRepresentation(
        @Schema(description = "ID único da mentoria", example = "150")
        Long id,

        @Schema(description = "ID do usuário mentor", example = "10")
        Long mentorId,

        @Schema(description = "Nome completo do mentor", example = "Carlos Andrade")
        String mentorName,

        @Schema(description = "ID da disciplina da mentoria", example = "25")
        Long disciplineId,

        @Schema(description = "Nome da disciplina", example = "Cálculo II")
        String disciplineName,

        @Schema(description = "Tipo da mentoria", example = "ONLINE")
        ClassType classType,

        @Schema(description = "Status atual da mentoria", example = "CONCLUIDA")
        StatusTutoring status,

        @Schema(description = "Data e hora de início da mentoria", example = "2024-05-20T14:00:00")
        LocalDateTime startTime,

        @Schema(description = "Data e hora de término da mentoria", example = "2024-05-20T15:30:00")
        LocalDateTime endTime,

        @Schema(description = "Local da mentoria (para tipo PRESENCIAL)", example = "Sala C101")
        String local,

        @Schema(description = "Link da sala virtual (para tipo ONLINE)", example = "https://meet.example.com/xyz-uvw-rst")
        String linkVideo,

        @Schema(description = "Número máximo de participantes permitido", example = "10")
        Integer maxParticipants,

        @Schema(description = "Indica se o chat está habilitado", example = "true")
        Boolean isChatEnable,

        @Schema(description = "Conjunto de informações dos participantes inscritos")
        Set<TutoringParticipantInfo> participants
) {}