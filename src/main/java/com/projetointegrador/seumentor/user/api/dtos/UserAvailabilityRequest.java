package com.projetointegrador.seumentor.user.api.dtos;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record UserAvailabilityRequest(
    @NotNull Long disciplineId,
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {}
