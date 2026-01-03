package com.banditgames.platform.chat.adapter.web.dto;

import com.banditgames.platform.chat.domain.Message;
import com.banditgames.platform.chat.domain.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Message response containing all details about a chat message")
public class MessageResponse {
    
    @Schema(
            description = "Unique identifier of the message. UUID format.",
            example = "550e8400-e29b-41d4-a716-446655440000",
            format = "uuid"
    )
    private UUID id;
    
    @Schema(
            description = "UUID of the user who sent the message.",
            example = "123e4567-e89b-12d3-a456-426614174001",
            format = "uuid"
    )
    private String senderId;
    
    @Schema(
            description = "UUID of the user who received the message.",
            example = "987fcdeb-51a2-43f1-b789-123456789abc",
            format = "uuid"
    )
    private String receiverId;
    
    @Schema(
            description = "The text content of the message. Maximum 2000 characters.",
            example = "Hey! Are you ready for the game tonight?"
    )
    private String content;
    
    @Schema(
            description = "Current status of the message. SENT: message sent but not delivered, DELIVERED: message delivered to recipient, READ: message has been read by recipient",
            example = "READ",
            allowableValues = {"SENT", "DELIVERED", "READ"}
    )
    private MessageStatus status;
    
    @Schema(
            description = "ISO 8601 timestamp when the message was sent",
            example = "2023-12-06T10:15:30",
            format = "date-time"
    )
    private LocalDateTime sentAt;
    
    @Schema(
            description = "ISO 8601 timestamp when the message was read by the recipient. Null if not yet read.",
            example = "2023-12-06T10:20:45",
            format = "date-time",
            nullable = true
    )
    private LocalDateTime readAt;
    
    public static MessageResponse fromDomain(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .status(message.getStatus())
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .build();
    }
}

