package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import com.projetointegrador.seumentor.user.api.UserQuery; 
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation; 
import com.projetointegrador.seumentor.user.model.User;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service("tutoringSecurityService")
@RequiredArgsConstructor
public class TutoringSecurityService {

    private static final Logger log = LoggerFactory.getLogger(TutoringSecurityService.class);
    private final TutoringRepository tutoringRepository;
    private final UserQuery userQuery;

    @Transactional(readOnly = true)
    public boolean isMentorOfTutoring(Authentication authentication, Long tutoringId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("isMentorOfTutoring: Authentication is null or not authenticated.");
            return false;
        }

        Object principal = authentication.getPrincipal();
        String userEmail;

        if (principal instanceof UserDetails) {
            userEmail = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            userEmail = (String) principal;
        } else {
            log.warn("isMentorOfTutoring: Principal is not an instance of UserDetails or String. Principal type: {}", principal.getClass().getName());
            return false;
        }

        Optional<UserRepresentation> authenticatedUserRepOpt = userQuery.findByEmail(userEmail);

        if (authenticatedUserRepOpt.isEmpty()) {
            log.warn("isMentorOfTutoring: Authenticated user with email {} not found via UserQuery.", userEmail);
            return false;
        }
        Long authenticatedUserId = authenticatedUserRepOpt.get().id();

        Optional<Tutoring> tutoringOpt = tutoringRepository.findById(tutoringId);
        if (tutoringOpt.isEmpty()) {
            log.debug("isMentorOfTutoring: Tutoring with ID {} not found.", tutoringId);
            return false;
        }

        Tutoring tutoring = tutoringOpt.get();
        User mentor = tutoring.getMentor();

        boolean isMentor = mentor != null && mentor.getId().equals(authenticatedUserId);
        log.debug("isMentorOfTutoring check for tutoringId {}: authenticatedUserId={}, mentorId={}, isMentor={}",
                tutoringId, authenticatedUserId, (mentor != null ? mentor.getId() : "null"), isMentor);
        return isMentor;
    }
}
