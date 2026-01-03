package com.banditgames.platform.chat.port.in;

import com.banditgames.platform.chat.domain.Message;

import java.util.UUID;

public interface SendMessageUseCase {
    /**
     * Send a message from one user to another
     * @param senderId The ID of the user sending the message
     * @param receiverId The ID of the user receiving the message
     * @param content The content of the message
     * @return The created message
     */
    Message sendMessage(String senderId, UUID receiverId, String content);
}

