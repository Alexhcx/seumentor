package com.projetointegrador.seumentor.tutoring.api;

import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import com.projetointegrador.seumentor.tutoring.exception.TutoringOperationException;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import org.springframework.security.access.AccessDeniedException; // Importar para exceção
import org.springframework.security.core.Authentication; // Importar

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

        void removeParticipant(Long tutoringId, Long userId, Authentication authentication);

        TutoringRepresentation cancelMentorTutoring(Long userId, Long tutoringId, boolean deactivateAvailability,
                        Authentication authentication) 
                        throws TutoringNotFoundException, TutoringOperationException, AccessDeniedException,
                        UserNotFoundException;

}
