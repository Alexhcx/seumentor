package com.projetointegrador.seumentor.tutoring.api.dto;

public record TutoringParticipantInfo(
        Long userId,
        String userName,
        String topic
) {}