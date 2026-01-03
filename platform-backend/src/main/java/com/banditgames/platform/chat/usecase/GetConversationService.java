package com.banditgames.platform.chat.usecase;

import com.banditgames.platform.chat.domain.Message;
import com.banditgames.platform.chat.domain.MessageStatus;
import com.banditgames.platform.chat.port.in.GetConversationUseCase;
import com.banditgames.platform.chat.port.out.LoadMessagePort;
import com.banditgames.platform.chat.port.out.SaveMessagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GetConversationService implements GetConversationUseCase {
    
    private final LoadMessagePort loadMessagePort;
    private final SaveMessagePort saveMessagePort;
    
    @Override
    public Page<Message> getConversation(String userId, String otherUserId, Pageable pageable) {
        // Get the conversation
        Page<Message> messages = loadMessagePort.findConversationBetweenUsers(userId, otherUserId, pageable);
        
        // Mark unread messages as read (only messages sent TO the authenticated user)
        List<Message> unreadMessages = loadMessagePort.findUnreadMessages(userId, otherUserId);
        
        for (Message message : unreadMessages) {
            if (message.getStatus() != MessageStatus.READ && message.getReceiverId().equals(userId)) {
                Message updatedMessage = Message.builder()
                        .id(message.getId())
                        .senderId(message.getSenderId())
                        .receiverId(message.getReceiverId())
                        .content(message.getContent())
                        .status(MessageStatus.READ)
                        .sentAt(message.getSentAt())
                        .readAt(LocalDateTime.now())
                        .build();
                saveMessagePort.save(updatedMessage);
            }
        }
        
        return messages;
    }
}

