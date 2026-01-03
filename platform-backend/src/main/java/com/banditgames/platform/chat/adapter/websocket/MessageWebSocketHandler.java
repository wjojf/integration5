package com.banditgames.platform.chat.adapter.websocket;

import com.banditgames.platform.chat.adapter.websocket.dto.WebSocketMessageNotification;
import com.banditgames.platform.chat.domain.events.MessageSentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageWebSocketHandler {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @EventListener
    public void handleMessageSentEvent(MessageSentEvent event) {
        try {
            WebSocketMessageNotification notification = WebSocketMessageNotification.fromDomain(event.getMessage());
            
            String receiverId = event.getMessage().getReceiverId();
            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/messages",
                    notification
            );
            
            log.debug("Sent WebSocket notification for message {} to user {}", 
                    notification.getMessageId(), receiverId);
        } catch (Exception e) {
            log.error("Error sending WebSocket notification for message", e);
        }
    }
}

