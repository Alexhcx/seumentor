package com.projetointegrador.seumentor.user.api;

import java.util.List;
import java.util.Optional;

import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.MentorProfileRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserAvailabilityRepresentation; // Importar
import com.projetointegrador.seumentor.user.api.dtos.UserProfileDetailedRepresentation;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.model.DayWeek;
import com.projetointegrador.seumentor.user.model.MentorAvailability;
import com.projetointegrador.seumentor.user.model.User;
import org.springframework.transaction.annotation.Transactional;

public interface UserQuery {

    @Transactional(readOnly = true)
    Optional<UserRepresentation> findById(Long userId);

    @Transactional(readOnly = true)
    Optional<UserRepresentation> findByEmail(String email);

    @Transactional
    User getUserReferenceById(Long userId);

    @Transactional(readOnly = true)
    List<UserRepresentation> findAllUserRepresentations();

    @Transactional(readOnly = true)
    Optional<UserAvailabilityRepresentation> findAvailabilityRepresentationById(Long availabilityId);

    @Transactional(readOnly = true)
    List<UserAvailabilityRepresentation> findAvailabilitiesRepresentationByUserId(Long userId);

    @Transactional(readOnly = true)
    Optional<MentorProfileRepresentation> findMentorProfileById(Long mentorId);

    @Transactional(readOnly = true)
    List<MentorProfileRepresentation> findAllMentorProfiles();

    @Transactional(readOnly = true)
    List<MentorAvailability> findAllAvailabilitiesByDayOfWeek(DayWeek dayOfWeek);

    @Transactional(readOnly = true)
    List<TutoringRepresentation> getUserMentoringSessions(Long userId);

    @Transactional(readOnly = true)
    List<TutoringRepresentation> getUserParticipationSessions(Long userId);
}

