package com.projetointegrador.seumentor.user.api.dtos;

import com.projetointegrador.seumentor.user.model.DayWeek;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record UserAvailabilityRequest(
    @NotNull Long disciplineId,
    @NotNull DayWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime
) {}
