package com.banditgames.platform.chat.usecase;

import com.banditgames.platform.chat.domain.Message;
import com.banditgames.platform.chat.domain.MessageStatus;
import com.banditgames.platform.chat.domain.events.MessageSentEvent;
import com.banditgames.platform.chat.port.in.SendMessageUseCase;
import com.banditgames.platform.chat.port.out.SaveMessagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SendMessageService implements SendMessageUseCase {
    
    private final SaveMessagePort saveMessagePort;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    public Message sendMessage(String senderId, UUID receiverId, String content) {
        if (senderId.equals(receiverId.toString())) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }
        
        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(receiverId.toString())
                .content(content)
                .status(MessageStatus.SENT)
                .sentAt(LocalDateTime.now())
                .build();
        
        Message savedMessage = saveMessagePort.save(message);
        
        eventPublisher.publishEvent(new MessageSentEvent(savedMessage));
        
        return savedMessage;
    }
}

