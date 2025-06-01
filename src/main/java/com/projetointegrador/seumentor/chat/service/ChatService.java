package com.projetointegrador.seumentor.chat.service;

import com.projetointegrador.seumentor.chat.dto.ChatInput;
import com.projetointegrador.seumentor.chat.dto.ChatOutputDTO;
import com.projetointegrador.seumentor.chat.enums.MessageType;
import com.projetointegrador.seumentor.chat.model.Conversations;
import com.projetointegrador.seumentor.chat.repository.ConversationsRepository;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.exception.TutoringNotFoundException;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationsRepository conversationsRepository;
    private final TutoringQuery tutoringQuery;
    private final UserQuery userQuery;

    @Transactional
    public ChatOutputDTO saveMessageAndBuildOutput(ChatInput input) {
        log.info("Salvando mensagem: {}", input);

        UserRepresentation senderRep = userQuery.findById(input.senderId())
                .orElseThrow(() -> new UserNotFoundException("Remetente não encontrado: " + input.senderId()));

        User senderRef = userQuery.getUserReferenceById(input.senderId());
        Tutoring tutoringRef = tutoringQuery.getTutoringReferenceById(input.tutoringId());
        Conversations.ConversationsBuilder conversationBuilder = Conversations.builder()
                .tutoring(tutoringRef)
                .sender(senderRef)
                .chatMessage(input.message())
                .messageType(input.type());

        UserRepresentation receiverRep = null;
        if (input.type() == MessageType.PRIVATE && input.receiverId() != null) {
            receiverRep = userQuery.findById(input.receiverId())
                    .orElseThrow(() -> new UserNotFoundException("Destinatário não encontrado: " + input.receiverId()));
            User receiverRef = userQuery.getUserReferenceById(input.receiverId());
            conversationBuilder.receiver(receiverRef);
        }

        Conversations savedConversation = conversationsRepository.save(conversationBuilder.build());
        log.info("Mensagem salva com ID: {}", savedConversation.getId());

        return ChatOutputDTO.builder()
                .id(savedConversation.getId())
                .tutoringId(input.tutoringId())
                .senderId(senderRep.id())
                .senderName(senderRep.firstName() + " " + senderRep.lastName())
                .senderFirstName(senderRep.firstName())
                .senderLastName(senderRep.lastName())
                .message(savedConversation.getChatMessage())
                .type(input.type())
                .receiverId(receiverRep != null ? receiverRep.id() : null)
                .receiverName(receiverRep != null ? receiverRep.firstName() + " " + receiverRep.lastName() : null)
                .timestamp(savedConversation.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public void validateChatMessage(Long tutoringId, ChatInput input) {
        TutoringRepresentation tutoringRep = tutoringQuery.findTutoringById(tutoringId)
                .orElseThrow(() -> {
                    log.warn("Mentoria não encontrada: {}", tutoringId);
                    return new TutoringNotFoundException("Mentoria não encontrada: " + tutoringId);
                });

        if (!Boolean.TRUE.equals(tutoringRep.isChatEnable())) {
            log.warn("Chat desabilitado para mentoria {}. Mensagem rejeitada.", tutoringId);
            throw new AccessDeniedException("Chat desabilitado para esta mentoria.");
        }

        boolean isSenderMentor = tutoringRep.mentorId().equals(input.senderId());
        boolean isSenderParticipant = tutoringRep.participants().stream()
                .anyMatch(p -> p.userId().equals(input.senderId()));

        if (!isSenderMentor && !isSenderParticipant) {
            log.warn("Usuário {} não é participante nem mentor da mentoria {}.", input.senderId(), tutoringId);
            throw new AccessDeniedException("Usuário não autorizado para este chat.");
        }

        if (input.type() == MessageType.GENERAL) {
            if (Boolean.TRUE.equals(tutoringRep.isMentorPostingOnly()) && !isSenderMentor) {
                log.warn("Chat em modo 'somente mentor' para mentoria {}. Mensagem rejeitada.", tutoringId);
                throw new AccessDeniedException("Apenas o mentor pode enviar mensagens gerais nesta mentoria.");
            }
        } else if (input.type() == MessageType.PRIVATE) {
            if (input.receiverId() == null) {
                log.warn("Mensagem privada sem receiverId para mentoria {}.", tutoringId);
                throw new IllegalArgumentException("Destinatário é obrigatório para mensagens privadas.");
            }

            boolean isReceiverMentor = tutoringRep.mentorId().equals(input.receiverId());
            boolean isReceiverParticipant = tutoringRep.participants().stream()
                    .anyMatch(p -> p.userId().equals(input.receiverId()));

            if (!((isSenderMentor && isReceiverParticipant) || (isSenderParticipant && isReceiverMentor))) {
                log.warn("Tentativa de chat privado inválida na mentoria {}.", tutoringId);
                throw new AccessDeniedException("Chat privado apenas entre mentor e participante.");
            }
        }
    }

    public String getDestinationTopic(Long tutoringId, MessageType type, Long senderId, Long receiverId) {
        if (type == MessageType.GENERAL|| type == MessageType.FILE) {
            return "/topic/tutoring/" + tutoringId + "/general";
        } else if (type == MessageType.PRIVATE && receiverId != null) {
            Long userAId = Math.min(senderId, receiverId);
            Long userBId = Math.max(senderId, receiverId);
            return "/topic/tutoring/" + tutoringId + "/private/" + userAId + "-" + userBId;
        }
        throw new IllegalArgumentException("Tipo de mensagem inválido ou dados insuficientes.");
    }

    @Transactional(readOnly = true)
    public List<ChatOutputDTO> getChatHistory(Long tutoringId) {
        log.info("Buscando histórico de chat para a mentoria ID: {}", tutoringId);

        tutoringQuery.findTutoringById(tutoringId)
                .orElseThrow(() -> new TutoringNotFoundException("Mentoria não encontrada com ID: " + tutoringId));

        List<Conversations> messages = conversationsRepository.findByTutoringIdOrderByCreatedAtAsc(tutoringId);

        return messages.stream().map(conversation -> {
            User sender = conversation.getSender();
            User receiver = conversation.getReceiver();

            UserRepresentation senderRep = userQuery.findById(sender.getId())
                    .orElse(new UserRepresentation(sender.getId(), sender.getFirstName(), sender.getLastName(),
                            sender.getEmail(), null, null, null, null, null, null, null, null, null, null)); // Fallback

            UserRepresentation receiverRep = null;
            if (receiver != null) {
                receiverRep = userQuery.findById(receiver.getId())
                        .orElse(new UserRepresentation(receiver.getId(), receiver.getFirstName(),
                                receiver.getLastName(), receiver.getEmail(), null, null, null, null, null, null, null,
                                null, null, null));
            }

            return ChatOutputDTO.builder()
                    .id(conversation.getId())
                    .tutoringId(conversation.getTutoring().getId())
                    .senderId(senderRep.id())
                    .senderName(senderRep.firstName() + " " + senderRep.lastName())
                    .senderFirstName(senderRep.firstName())
                    .senderLastName(senderRep.lastName())
                    .message(conversation.getChatMessage())
                    .type(conversation.getMessageType())
                    .receiverId(receiverRep != null ? receiverRep.id() : null)
                    .receiverName(receiverRep != null ? receiverRep.firstName() + " " + receiverRep.lastName() : null)
                    .timestamp(conversation.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}