package com.projetointegrador.seumentor.user.api.dtos;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UserAvailabilityRepresentation(
    Long id,
    SimpleDisciplineRepresentation discipline,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {}
