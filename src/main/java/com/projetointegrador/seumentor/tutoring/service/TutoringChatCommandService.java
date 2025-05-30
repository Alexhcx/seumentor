package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.tutoring.api.TutoringChatCommand;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation; // Import UserRepresentation
import com.projetointegrador.seumentor.user.enums.Role;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TutoringChatCommandService implements TutoringChatCommand {

    private static final Logger log = LoggerFactory.getLogger(TutoringChatCommandService.class);

    private final TutoringRepository tutoringRepository;
    private final UserQuery userQuery; // Para obter informações do usuário autenticado
    private final TutoringQuery tutoringQuery; // Para obter a representação atualizada

    @Override
    @Transactional
    public TutoringRepresentation setChatEnabled(Long tutoringId, boolean enable, Authentication authentication)
            throws TutoringNotFoundException, AccessDeniedException {
        log.info("Service: User {} attempting to set chat enabled to {} for tutoring ID: {}",
                authentication.getName(), enable, tutoringId);

        Tutoring tutoring = tutoringRepository.findById(tutoringId)
                .orElseThrow(() -> {
                    log.warn("Service: setChatEnabled failed. Tutoring not found with ID: {}", tutoringId);
                    return new TutoringNotFoundException("Mentoria não encontrada com ID: " + tutoringId);
                });

        // A verificação de permissão @PreAuthorize no controller já deve ter coberto isso,
        // mas uma verificação adicional aqui pode ser feita por segurança (defesa em profundidade).
        // Por exemplo, usando o tutoringSecurityService.isMentorOfTutoring ou verificando o role ADMIN.
        // Para este exemplo, vamos assumir que @PreAuthorize é suficiente.

        if (tutoring.getIsChatEnable() == enable) {
            log.info("Service: Chat for tutoring ID {} is already {}. No changes made.", tutoringId, enable ? "enabled" : "disabled");
            // Retorna a representação atual sem salvar novamente se não houver mudança.
            return tutoringQuery.findTutoringById(tutoringId)
                    .orElseThrow(() -> new IllegalStateException("Falha ao buscar mentoria: " + tutoringId + " após verificar status do chat."));
        }

        tutoring.setIsChatEnable(enable);
        Tutoring updatedTutoring = tutoringRepository.save(tutoring);
        log.info("Service: Chat enabled status for tutoring ID {} successfully set to {} by user {}",
                updatedTutoring.getId(), enable, authentication.getName());

        return tutoringQuery.findTutoringById(updatedTutoring.getId())
                .orElseThrow(() -> new IllegalStateException("Falha ao buscar mentoria recém-atualizada: " + updatedTutoring.getId()));
    }

    @Override
    @Transactional
    public TutoringRepresentation setMentorPostingOnly(Long tutoringId, boolean mentorOnly, Authentication authentication)
            throws TutoringNotFoundException, AccessDeniedException {
        log.info("Service: User {} attempting to set mentorPostingOnly to {} for tutoring ID: {}",
                authentication.getName(), mentorOnly, tutoringId);

        Tutoring tutoring = tutoringRepository.findById(tutoringId)
                .orElseThrow(() -> {
                    log.warn("Service: setMentorPostingOnly failed. Tutoring not found with ID: {}", tutoringId);
                    return new TutoringNotFoundException("Mentoria não encontrada com ID: " + tutoringId);
                });

        // Similar ao setChatEnabled, @PreAuthorize já fez a verificação principal.

        if (tutoring.getIsMentorPostingOnly() == mentorOnly) {
            log.info("Service: MentorPostingOnly for tutoring ID {} is already {}. No changes made.", tutoringId, mentorOnly);
             return tutoringQuery.findTutoringById(tutoringId)
                    .orElseThrow(() -> new IllegalStateException("Falha ao buscar mentoria: " + tutoringId + " após verificar status de postagem do mentor."));
        }

        tutoring.setIsMentorPostingOnly(mentorOnly);
        Tutoring updatedTutoring = tutoringRepository.save(tutoring);
        log.info("Service: MentorPostingOnly status for tutoring ID {} successfully set to {} by user {}",
                updatedTutoring.getId(), mentorOnly, authentication.getName());

        return tutoringQuery.findTutoringById(updatedTutoring.getId())
                .orElseThrow(() -> new IllegalStateException("Falha ao buscar mentoria recém-atualizada: " + updatedTutoring.getId()));
    }

    // Método auxiliar para verificar permissões (exemplo, pode ser mais complexo ou usar um serviço dedicado)
    // Este método não é estritamente necessário se @PreAuthorize e @tutoringSecurityService.isMentorOfTutoring
    // já cobrem os casos no controller. Mantido aqui como exemplo de lógica de serviço.
    private void checkPermission(Tutoring tutoring, Authentication authentication) throws AccessDeniedException, UserNotFoundException {
        String username = authentication.getName();
        Optional<UserRepresentation> userRepOpt = userQuery.findByEmail(username);

        if (userRepOpt.isEmpty()) {
            log.error("Service: Permission check failed. Authenticated user {} not found via UserQuery.", username);
            throw new UserNotFoundException("Usuário autenticado não encontrado: " + username);
        }
        User authenticatedUser = userQuery.getUserReferenceById(userRepOpt.get().id());


        boolean isAdmin = authenticatedUser.getRole() == Role.ADMIN;
        boolean isMentorOfTutoring = tutoring.getMentor() != null && tutoring.getMentor().getId().equals(authenticatedUser.getId());

        if (!isAdmin && !isMentorOfTutoring) {
            log.warn("Service: User {} (Role: {}) is not authorized to modify chat settings for tutoring ID {} (Mentor ID: {}).",
                    username, authenticatedUser.getRole(), tutoring.getId(), (tutoring.getMentor() != null ? tutoring.getMentor().getId() : "null"));
            throw new AccessDeniedException("Usuário não autorizado a modificar as configurações do chat desta mentoria.");
        }
        log.debug("Service: User {} has permission to modify chat settings for tutoring ID {}.", username, tutoring.getId());
    }
}