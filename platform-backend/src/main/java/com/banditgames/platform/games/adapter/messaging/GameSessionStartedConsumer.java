package com.banditgames.platform.games.adapter.messaging;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes game session started events from RabbitMQ and updates lobby.
 * 
 * Architecture: Game Service publishes events, Platform Backend consumes them.
 * No direct REST calls between services.
 * 
 * Flow:
 * 1. Game Service publishes: game.session.started
 * 2. This consumer updates lobby with session_id
 * 3. Updates lobby status to ACTIVE
 * 4. (Optional) Publishes WebSocket event to frontend
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionStartedConsumer {

    private final LoadLobbyPort loadLobbyPort;
    private final SaveLobbyPort saveLobbyPort;
    
    @Value("${game.events.queues.session-started:game.session.started}")
    private String sessionStartedQueue;
    
    /**
     * Consumes game session started events and updates lobby.
     * 
     * @param event The game session started event
     */
    @RabbitListener(queues = "${game.events.queues.session-started}")
    public void onGameSessionStarted(Map<String, Object> event) {
        try {
            String sessionIdStr = (String) event.get("session_id");
            String lobbyIdStr = (String) event.get("lobby_id");
            String gameType = (String) event.get("game_type");
            String status = (String) event.get("status");
            
            if (sessionIdStr == null || lobbyIdStr == null) {
                log.warn("Received game.session.started event without session_id or lobby_id: {}", event);
                return;
            }
            
            UUID sessionId = UUID.fromString(sessionIdStr);
            UUID lobbyId = UUID.fromString(lobbyIdStr);
            
            log.info("Processing game session started - lobbyId={}, sessionId={}, gameType={}, status={}", 
                    lobbyId, sessionId, gameType, status);
            
            // Load lobby
            Lobby lobby = loadLobbyPort.findById(lobbyId)
                    .orElseThrow(() -> new RuntimeException("Lobby not found: " + lobbyId));
            
            // Update lobby with session ID and set status to IN_PROGRESS
            // Note: Lobby is in STARTED status when lobby.start() is called
            // When session actually starts, we transition to IN_PROGRESS to indicate game is active
            lobby.setSessionId(sessionId);
            // Set status to IN_PROGRESS to indicate the game session is now active
            if (lobby.getStatus() == com.banditgames.platform.lobby.domain.LobbyStatus.STARTED) {
                lobby.setStatus(com.banditgames.platform.lobby.domain.LobbyStatus.IN_PROGRESS);
            }
            
            // Save the updated lobby
            saveLobbyPort.save(lobby);
            
            log.info("Game session started for lobby - lobbyId={}, sessionId={}, gameType={}, status={}", 
                    lobbyId, sessionId, gameType, status);
            
        } catch (Exception e) {
            log.error("Error processing game session started event: {}", event, e);
            // Don't throw - event processing failures shouldn't break the system
            // Consider publishing to DLQ for retry
        }
    }
}
