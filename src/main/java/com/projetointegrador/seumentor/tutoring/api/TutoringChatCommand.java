package com.projetointegrador.seumentor.tutoring.api;

import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

public interface TutoringChatCommand {

    TutoringRepresentation setChatEnabled(Long tutoringId, boolean enable, Authentication authentication)
            throws TutoringNotFoundException, AccessDeniedException;

    TutoringRepresentation setMentorPostingOnly(Long tutoringId, boolean mentorOnly, Authentication authentication)
            throws TutoringNotFoundException, AccessDeniedException;
}
