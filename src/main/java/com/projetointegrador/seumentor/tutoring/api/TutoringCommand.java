// src/main/java/com/projetointegrador/seumentor/tutoring/api/TutoringCommand.java
package com.projetointegrador.seumentor.tutoring.api;

import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.tutoring.exception.AvailabilityNotFoundException; // Adicionado
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface TutoringCommand {

        ScheduledTutoringRepresentation scheduleTutoring(ScheduleTutoringRequest request) throws Exception;

        TutoringRepresentation confirmAndUpdatetutoring(Long tutoringId, ConfirmTutoringRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException;

        TutoringRepresentation updateTutoringStatus(Long tutoringId, UpdateTutoringStatusRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException;

        TutoringRepresentation addParticipant(Long tutoringId, AddParticipantRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, UserNotFoundException, TutoringOperationException,
                        AccessDeniedException;

        TutoringRatingRepresentation addTutoringRating(Long tutoringId, TutoringRatingRequest request,
                        Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException,
                        UserNotFoundException;

        void deleteOrCancelTutoring(Long tutoringId, Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException;

        void deleteTutoringRating(Long ratingId) throws TutoringOperationException;

        void removeParticipant(Long tutoringId, Long userId, Authentication authentication)
                        throws TutoringNotFoundException, UserNotFoundException, TutoringOperationException,
                        AccessDeniedException;

        TutoringRepresentation cancelMentorTutoring(Long userId, Long tutoringId, boolean deactivateAvailability,
                        Authentication authentication)
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException,
                        UserNotFoundException;


        UserAvailabilityRepresentation addMentorAvailability(Long mentorId, UserAvailabilityRequest request)
                        throws UserNotFoundException, TutoringOperationException;

        List<UserAvailabilityRepresentation> updateMentorAvailabilityStatus(Long mentorId, Long availabilityId,
                        UpdateAvailabilityStatusRequest request)
                        throws UserNotFoundException, AvailabilityNotFoundException, TutoringOperationException,
                        AccessDeniedException;

        void deleteMentorAvailability(Long mentorId, Long availabilityId)
                        throws UserNotFoundException, AvailabilityNotFoundException, TutoringOperationException,
                        AccessDeniedException;
}