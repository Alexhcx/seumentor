package com.projetointegrador.seumentor.tutoring.api.dto;

import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List; 

@Schema(description = "Representação dos dados de uma mentoria recém-agendada")
public record ScheduledTutoringRepresentation(
        @Schema(description = "ID único da mentoria", example = "151")
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
        TutoringClassType tutoringClassType,

        @Schema(description = "Status atual da mentoria (normalmente PENDENTE após agendamento)", example = "PENDENTE")
        StatusTutoring status,

        @Schema(description = "Horário de início da mentoria (HH:mm)", type = "string", example = "14:00")
        String startTime,

        @Schema(description = "Horário de término da mentoria (HH:mm)", type = "string", example = "15:30")
        String endTime,

        @Schema(description = "Data da mentoria (dd/MM/yyyy)", type = "string", example = "20/05/2025")
        String tutoringDate,

        @Schema(description = "Local da mentoria (PRESENCIAL: 'A definir', ONLINE: 'Não se aplica')", example = "A definir")
        String local,

        @Schema(description = "Link da sala virtual (ONLINE: 'Não se aplica' ou link, PRESENCIAL: 'Não se aplica')", example = "Não se aplica")
        String linkVideo,

        @Schema(description = "Número máximo de participantes permitido (pode ser null inicialmente)", example = "10")
        Integer maxParticipants,

        @Schema(description = "Quantidade atual de participantes na mentoria", example = "1")
        Integer qtdParticipants,

        @Schema(description = "Indica se o chat está habilitado (normalmente false inicialmente)", example = "false")
        Boolean isChatEnable,

        @Schema(description = "Lista de tópicos de interesse dos participantes para a mentoria")
        List<String> topics
) {}