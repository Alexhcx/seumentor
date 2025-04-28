package com.projetointegrador.seumentor.tutoring.api.dto;

import com.projetointegrador.seumentor.tutoring.model.ClassType;
import com.projetointegrador.seumentor.tutoring.model.StatusTutoring;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record TutoringRepresentation(
        Long id,
        Long mentorId,
        String mentorName,
        Long disciplineId,
        String disciplineName,
        ClassType classType,
        StatusTutoring status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String local,
        String linkVideo,
        Integer maxParticipants,
        Boolean isChatEnable,
        Set<TutoringParticipantInfo> participants
) {}