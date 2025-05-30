package com.projetointegrador.seumentor.tutoring.api.dto;

import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringClassType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Representação completa dos dados de uma mentoria")
public record TutoringRepresentation(
                @Schema(description = "ID único da mentoria", example = "150") Long id,

                @Schema(description = "ID do usuário mentor", example = "10") Long mentorId,

                @Schema(description = "Nome completo do mentor", example = "Carlos Andrade") String mentorName,

                @Schema(description = "ID da disciplina da mentoria", example = "25") Long disciplineId,

                @Schema(description = "Nome da disciplina", example = "Cálculo II") String disciplineName,

                @Schema(description = "Tipo da mentoria", example = "ONLINE") TutoringClassType tutoringClassType,

                @Schema(description = "Status atual da mentoria", example = "CONCLUIDA") StatusTutoring status,

                @Schema(description = "Horário de início da mentoria (HH:mm)", type = "string", example = "14:00") String startTime,

                @Schema(description = "Horário de término da mentoria (HH:mm)", type = "string", example = "15:30") String endTime,

                @Schema(description = "Data da mentoria (dd/MM/yyyy)", type = "string", example = "03/12/2007") String tutoringDate,

                @Schema(description = "Define se só o mentor pode mandar mensagem", type = "boolean", example = "true") Boolean isMentorPostingOnly,

                @Schema(description = "Local da mentoria (para tipo PRESENCIAL)", example = "Sala C101") String local,

                @Schema(description = "Link da sala virtual (para tipo ONLINE)", example = "https://meet.example.com/xyz-uvw-rst") String linkVideo,

                @Schema(description = "Número máximo de participantes permitido", example = "10") Integer maxParticipants,

                @Schema(description = "Quantidade atual de participantes na mentoria", example = "1") Integer qtdParticipants,

                @Schema(description = "Indica se o chat está habilitado", example = "true") Boolean isChatEnable,

                @Schema(description = "Conjunto de informações dos participantes inscritos") Set<TutoringParticipantInfo> participants) {
}