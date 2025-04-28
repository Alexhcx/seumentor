package com.projetointegrador.seumentor.tutoring.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import com.projetointegrador.seumentor.tutoring.model.ClassType;

public record ScheduleTutoringRequest(
        @NotNull
        Long mentorId,

        @NotNull
        Long disciplineId,

        @NotNull
        Long menteeId,

        @NotBlank
        String topic,

        @NotNull
        LocalDate tutoringDate,

        @NotNull
        LocalTime startTime,

        @NotNull
        LocalTime endTime,

        @NotNull
        ClassType classType
) {}
