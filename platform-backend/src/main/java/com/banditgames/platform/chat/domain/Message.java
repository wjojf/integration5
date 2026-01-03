package com.banditgames.platform.chat.domain;

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
public class Message {
    private UUID id;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageStatus status;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}

