package com.banditgames.platform.achievements.adapter.messaging;

import com.banditgames.platform.acl.port.out.GameContextPort;
import com.banditgames.platform.achievements.adapter.messaging.messages.AchievementAcquiredMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Event consumer for achievement-related messages from external game service.
 * 
 * This consumer delegates to the ACL adapter (GameEventsAdapter) which handles
 * the translation between external message formats and the Achievements context.
 * 
 * Note: The actual message handling is done in GameEventsAdapter as part of the ACL pattern.
 * This consumer is kept for backward compatibility but should be migrated to use ACL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementEventConsumer {

    private final GameContextPort gameContextPort;

    /**
     * Handles achievement messages from game service move responses queue.
     * This is a legacy endpoint - new implementations should use GameEventsAdapter.
     */
    @RabbitListener(queues = "${game.events.queues.move-responses}")
    public void handleAchievementFromMoveResponse(AchievementAcquiredMessage message) {
        log.debug("Received achievement message from move-responses queue: {}", message);
        
        // Delegate to ACL for proper translation
        if (message != null && message.getPlayerId() != null && message.getGameId() != null) {
            gameContextPort.handleThirdPartyAchievementUnlocked(
                    message.getGameId(),
                    message.getPlayerId(),
                    message.getAchievementType(),
                    message.getAchievementType(),
                    message.getAchievementDescription()
            );
        }
    }
}
