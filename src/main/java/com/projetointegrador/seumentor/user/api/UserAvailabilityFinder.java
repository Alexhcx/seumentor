package com.projetointegrador.seumentor.user.api;

import com.projetointegrador.seumentor.user.model.DayWeek;
import com.projetointegrador.seumentor.user.model.MentorAvailability;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserAvailabilityFinder {

    @Transactional(readOnly = true)
    List<MentorAvailability> findAllAvailabilitiesByDayOfWeek(DayWeek dayOfWeek);
}
