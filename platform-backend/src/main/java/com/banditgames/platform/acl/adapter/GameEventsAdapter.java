package com.banditgames.platform.acl.adapter;

import com.banditgames.platform.acl.port.out.GameContextPort;
import com.banditgames.platform.achievements.adapter.messaging.messages.AchievementAcquiredMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Anti-Corruption Layer (ACL) adapter for external Game service events.
 * 
 * This adapter translates external RabbitMQ messages from the game service
 * into consuming contexts' domain language, ensuring loose coupling
 * and protecting contexts from external API changes.
 * 
 * The ACL pattern isolates consuming contexts from the external
 * game service's message format and structure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventsAdapter {

    private final GameContextPort gameContextPort;

    /**
     * Handles achievement acquired messages from external game service.
     * Translates external message format to consuming context format.
     */
    @RabbitListener(queues = "${game.events.queues.achievements}")
    public void handleAchievementAcquired(AchievementAcquiredMessage message) {
        log.debug("ACL: Received achievement acquired message: {}", message);
        
        try {
            // Translate external message format to consuming context format
            gameContextPort.handleThirdPartyAchievementUnlocked(
                    message.getGameId(),
                    message.getPlayerId(),
                    message.getAchievementType(), // Using achievementType as code
                    message.getAchievementType(), // Using achievementType as name
                    message.getAchievementDescription()
            );
        } catch (Exception e) {
            log.error("Error processing achievement acquired message: {}", message, e);
            // Don't throw - ACL should handle errors gracefully to prevent message loss
        }
    }
}

