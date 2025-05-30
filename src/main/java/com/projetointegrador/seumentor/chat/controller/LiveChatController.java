package com.projetointegrador.seumentor.chat.controller;

import com.projetointegrador.seumentor.chat.dto.ChatInput;
import com.projetointegrador.seumentor.chat.dto.ChatOutput;
import com.projetointegrador.seumentor.chat.enums.MessageType; 
import com.projetointegrador.seumentor.chat.model.Conversations;
import com.projetointegrador.seumentor.chat.repository.ConversationsRepository;

import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import com.projetointegrador.seumentor.tutoring.model.Tutoring; 

import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.model.User;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LiveChatController {

    private static final Logger log = LoggerFactory.getLogger(LiveChatController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationsRepository conversationsRepository;
    private final TutoringQuery tutoringQuery;
    private final UserQuery userQuery;

    @MessageMapping("/chat/tutoring/{tutoringId}/send")
    public void sendTutoringMessage(@DestinationVariable Long tutoringId, @Payload ChatInput input) {
        log.info("Mensagem recebida para mentoria {}: {}", tutoringId, input);

        if (!tutoringId.equals(input.tutoringId())) {
            log.warn("Discrepância de tutoringId no path ({}) e payload ({}). Rejeitando.", tutoringId, input.tutoringId());
            return;
        }

        // 1. Busca TutoringRepresentation usando TutoringQuery
        //    Assume-se que TutoringRepresentation contém isChatEnable, isMentorPostingOnly, mentorId, e participants().
        TutoringRepresentation tutoringRep = tutoringQuery.findTutoringById(tutoringId)
                .orElseThrow(() -> {
                    log.warn("Mentoria não encontrada via TutoringQuery: {}", tutoringId);
                    return new TutoringNotFoundException("Mentoria não encontrada: " + tutoringId);
                });

        // 2. Busca a entidade User (ou referência) usando UserQuery
        User sender = userQuery.getUserReferenceById(input.senderId());
        // Se getUserReferenceById lançar UserNotFoundException, não é necessário checar por null.

        // Regra 1: O chat está habilitado para esta mentoria?
        if (!Boolean.TRUE.equals(tutoringRep.isChatEnable())) {
            log.warn("Chat desabilitado para mentoria {}. Mensagem de {} rejeitada.", tutoringId, sender.getEmail());
            return;
        }

        // Regra 2: O usuário é participante ou mentor?
        boolean isSenderMentor = tutoringRep.mentorId().equals(sender.getId());
        boolean isSenderParticipant = tutoringRep.participants().stream()
                .anyMatch(p -> p.userId().equals(sender.getId()));

        if (!isSenderMentor && !isSenderParticipant) {
            log.warn("Usuário {} não é participante nem mentor da mentoria {}. Mensagem rejeitada.", sender.getEmail(), tutoringId);
            throw new AccessDeniedException("Usuário não autorizado para este chat.");
        }

        String destination;
        // Para salvar, precisamos da referência da entidade Tutoring.
        // Assume-se que TutoringQuery tem um método getTutoringReferenceById.
        Tutoring tutoringEntityRef = tutoringQuery.getTutoringReferenceById(tutoringId);

        Conversations conversationToSave = Conversations.builder()
                .tutoring(tutoringEntityRef)
                .sender(sender)
                .chatMessage(HtmlUtils.htmlEscape(input.message()))
                .messageType(input.type()) // ChatInput deve ter o campo 'type' do tipo MessageType
                .build();

        if (input.type() == MessageType.GENERAL) {
            // Regra 3: Se o modo "somente mentor" está ativo, o remetente é o mentor?
            if (Boolean.TRUE.equals(tutoringRep.isMentorPostingOnly()) && !isSenderMentor) {
                log.warn("Chat em modo 'somente mentor' para mentoria {}. Mensagem de {} (não mentor) rejeitada.", tutoringId, sender.getEmail());
                return;
            }
            destination = "/topic/tutoring/" + tutoringId + "/general";
        } else if (input.type() == MessageType.PRIVATE) {
            if (input.receiverId() == null) {
                log.warn("Mensagem privada sem receiverId para mentoria {}. Rejeitando.", tutoringId);
                return;
            }
            User receiver = userQuery.getUserReferenceById(input.receiverId());

            // Regra 4: Para mensagens privadas, garantir que seja entre mentor e um participante.
            boolean isReceiverMentor = tutoringRep.mentorId().equals(receiver.getId());
            boolean isReceiverParticipant = tutoringRep.participants().stream()
                    .anyMatch(p -> p.userId().equals(receiver.getId()));

            if (!((isSenderMentor && isReceiverParticipant) || (isSenderParticipant && isReceiverMentor))) {
                log.warn("Tentativa de chat privado inválida na mentoria {}. Remetente: {}, Destinatário: {}. MentorID: {}",
                        tutoringId, sender.getEmail(), receiver.getEmail(), tutoringRep.mentorId());
                throw new AccessDeniedException("Chat privado apenas entre mentor e participante.");
            }

            conversationToSave.setReceiver(receiver);
            // Cria um nome de tópico consistente para chats privados ordenando os IDs
            Long userAId = Math.min(sender.getId(), receiver.getId());
            Long userBId = Math.max(sender.getId(), receiver.getId());
            destination = "/topic/tutoring/" + tutoringId + "/private/" + userAId + "-" + userBId;
        } else {
            log.error("Tipo de mensagem desconhecido: {}", input.type());
            return;
        }

        try {
            conversationsRepository.save(conversationToSave);
            log.info("Mensagem salva: {}", conversationToSave.getId());
        } catch (Exception e) {
            log.error("Erro ao salvar mensagem da mentoria {}: {}", tutoringId, e.getMessage(), e);
            // Considere se deve prosseguir com o envio se o salvamento no BD falhar
        }

        // ChatOutput pode ser enriquecido se necessário (ex: nome completo do remetente, timestamp)
        ChatOutput output = new ChatOutput(HtmlUtils.htmlEscape(sender.getFirstName() + ": " + input.message()));
        messagingTemplate.convertAndSend(destination, output);
        log.info("Mensagem enviada para {}: {}", destination, output);
    }
}
