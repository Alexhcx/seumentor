package com.projetointegrador.seumentor.user.api.dtos;

import com.projetointegrador.seumentor.user.model.DayWeek;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UserAvailabilityRepresentation(
    Long id,
    SimpleDisciplineRepresentation discipline,
    DayWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {}
