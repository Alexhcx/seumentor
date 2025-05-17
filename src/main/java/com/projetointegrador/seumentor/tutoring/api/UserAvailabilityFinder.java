package com.projetointegrador.seumentor.tutoring.api;

import com.projetointegrador.seumentor.common.enums.DayWeek;
import com.projetointegrador.seumentor.tutoring.model.MentorAvailability;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserAvailabilityFinder {

    @Transactional(readOnly = true)
    List<MentorAvailability> findAllAvailabilitiesByDayOfWeek(DayWeek dayOfWeek);
}
