package com.banditgames.platform.acl.adapter;

import com.banditgames.platform.acl.port.out.GameContextPort;
import com.banditgames.platform.shared.events.GameEndedDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Event listener for game ended events from the Game/Lobby context.
 * 
 * This listener uses the ACL pattern to translate shared domain events
 * into consuming context actions, ensuring loose coupling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEndedEventListener {

    private final GameContextPort gameContextPort;

    /**
     * Handles game ended events published by the Game/Lobby context.
     * Translates the event through the ACL to maintain context boundaries.
     */
    @ApplicationModuleListener
    public void onGameEnded(GameEndedDomainEvent event) {
        log.debug("ACL: Received GameEndedDomainEvent for lobby: {}, winner: {}", 
                event.lobbyId(), event.winnerId());

        try {
            // Translate shared event to consuming context format
            // Note: We don't have playerIds in the event, so we pass empty list
            // In a real implementation, you might want to enrich the event or query for participants
            gameContextPort.handleGameEnded(
                    event.lobbyId(),
                    event.winnerId(),
                    Collections.emptyList() // TODO: Enrich event with participant IDs if needed
            );
        } catch (Exception e) {
            log.error("Error handling game ended event for lobby: {}", event.lobbyId(), e);
            // Don't throw - this is an async event handler
        }
    }
}

