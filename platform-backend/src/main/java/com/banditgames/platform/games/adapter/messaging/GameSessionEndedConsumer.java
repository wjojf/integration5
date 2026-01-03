package com.banditgames.platform.games.adapter.messaging;

import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.shared.events.GameEndedDomainEvent;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes game session ended events from RabbitMQ and publishes GameEndedDomainEvent.
 * 
 * Architecture: Game Service publishes events, Platform Backend consumes them.
 * Game-service doesn't know about lobbies, so we query by sessionId to find the lobby.
 * 
 * Flow:
 * 1. Game Service publishes: game.session.ended (with session_id, no lobby_id)
 * 2. This consumer queries lobby by sessionId
 * 3. Publishes GameEndedDomainEvent with the found lobbyId
 * 4. GameEventListener handles the event and clears sessionId from lobby
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionEndedConsumer {

    private final PlatformEventPublisher eventPublisher;
    private final LoadLobbyPort loadLobbyPort;
    
    /**
     * Consumes game session ended events and publishes GameEndedDomainEvent.
     * 
     * @param event The game session ended event from game-service
     */
    @RabbitListener(queues = "${game.events.queues.session-ended}")
    public void onGameSessionEnded(Map<String, Object> event) {
        try {
            String sessionIdStr = (String) event.get("session_id");
            String status = (String) event.get("status");
            String winnerIdStr = (String) event.get("winner_id");
            
            if (sessionIdStr == null) {
                log.warn("Received game.session.ended event without session_id: {}", event);
                return;
            }
            
            UUID sessionId;
            try {
                sessionId = UUID.fromString(sessionIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid session_id format in game.session.ended event: {}", sessionIdStr);
                return;
            }
            
            // Query lobby by sessionId (game-service doesn't know about lobbies)
            var lobbyOpt = loadLobbyPort.findBySessionId(sessionId);
            if (lobbyOpt.isEmpty()) {
                log.warn("No lobby found for sessionId: {} - this might be an AI game or session without lobby", sessionIdStr);
                return;
            }
            
            var lobby = lobbyOpt.get();
            UUID lobbyId = lobby.getId();
            
            UUID winnerId = null;
            if (winnerIdStr != null && !winnerIdStr.isEmpty()) {
                try {
                    winnerId = UUID.fromString(winnerIdStr);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid winner_id format in game.session.ended event: {}", winnerIdStr);
                }
            }
            
            log.info("Processing game session ended - lobbyId={}, sessionId={}, status={}, winnerId={}", 
                    lobbyId, sessionIdStr, status, winnerId);
            
            // Publish GameEndedDomainEvent to trigger lobby cleanup
            GameEndedDomainEvent domainEvent = new GameEndedDomainEvent(lobbyId, winnerId);
            eventPublisher.publish(domainEvent);
            
            log.info("Published GameEndedDomainEvent for lobby: {}, winner: {}", lobbyId, winnerId);
            
        } catch (Exception e) {
            log.error("Error processing game session ended event: {}", event, e);
            // Don't throw - event processing failures shouldn't break the system
        }
    }
}

