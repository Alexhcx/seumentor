package com.projetointegrador.seumentor.chat.dto;

import com.projetointegrador.seumentor.chat.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatOutputDTO {
    private Long id;
    private Long tutoringId;
    private Long senderId;
    private String senderName;
    private String senderFirstName;
    private String senderLastName;
    private String message;
    private MessageType type;
    private Long receiverId;
    private String receiverName;
    private LocalDateTime timestamp;
}
