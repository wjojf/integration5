package com.banditgames.platform.chat.adapter.websocket.dto;

import com.banditgames.platform.chat.domain.Message;
import com.banditgames.platform.chat.domain.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageNotification {
    private UUID messageId;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageStatus status;
    private LocalDateTime sentAt;
    
    public static WebSocketMessageNotification fromDomain(Message message) {
        return WebSocketMessageNotification.builder()
                .messageId(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .status(message.getStatus())
                .sentAt(message.getSentAt())
                .build();
    }
}

