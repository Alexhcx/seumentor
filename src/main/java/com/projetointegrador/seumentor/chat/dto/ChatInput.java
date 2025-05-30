package com.projetointegrador.seumentor.chat.dto;

import com.projetointegrador.seumentor.chat.enums.MessageType;

public record ChatInput(
    Long tutoringId,       
    Long senderId,         
    String message,        
    MessageType type,      
    Long receiverId       
) {
    public ChatInput(Long tutoringId, Long senderId, String message) {
        this(tutoringId, senderId, message, MessageType.GENERAL, null);
    }

    public ChatInput(Long tutoringId, Long senderId, String message, Long receiverId) {
        this(tutoringId, senderId, message, MessageType.PRIVATE, receiverId);
    }
}
