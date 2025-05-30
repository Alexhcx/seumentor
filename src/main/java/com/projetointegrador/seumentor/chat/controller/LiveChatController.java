package com.projetointegrador.seumentor.chat.controller;

import com.projetointegrador.seumentor.chat.dto.ChatInput;
import com.projetointegrador.seumentor.chat.dto.ChatOutputDTO;
import com.projetointegrador.seumentor.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LiveChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat/tutoring/{tutoringId}/send")
    public void sendTutoringMessage(@DestinationVariable Long tutoringId, @Payload ChatInput input) {
        log.info("Mensagem recebida para mentoria {}: {}", tutoringId, input);

        if (!tutoringId.equals(input.tutoringId())) {
            log.warn("Discrepância de tutoringId no path ({}) e payload ({}). Rejeitando.", 
                    tutoringId, input.tutoringId());
            return;
        }

        try {
            chatService.validateChatMessage(tutoringId, input);
            
            ChatOutputDTO output = chatService.saveMessageAndBuildOutput(input);
            
            String destination = chatService.getDestinationTopic(
                    tutoringId, 
                    input.type(), 
                    input.senderId(), 
                    input.receiverId()
            );
            
            messagingTemplate.convertAndSend(destination, output);
            log.info("Mensagem enviada para {}: {}", destination, output);
            
        } catch (AccessDeniedException e) {
            log.warn("Acesso negado ao enviar mensagem: {}", e.getMessage());
            sendErrorToUser(input.senderId(), e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao processar mensagem da mentoria {}: {}", tutoringId, e.getMessage(), e);
            sendErrorToUser(input.senderId(), "Erro ao processar mensagem");
        }
    }
    
    private void sendErrorToUser(Long userId, String errorMessage) {
        try {
            String errorDestination = "/topic/user/" + userId + "/errors";
            messagingTemplate.convertAndSend(errorDestination, new ErrorMessage(errorMessage));
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem de erro para usuário {}: {}", userId, e.getMessage());
        }
    }
    
    private record ErrorMessage(String message) {}
}