package com.banditgames.platform.games.adapter.messaging;

import com.banditgames.platform.lobby.domain.events.LobbyStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes game session start requests to RabbitMQ when a lobby is started.
 * 
 * Architecture: Platform Backend publishes events, Game Service consumes them.
 * No direct REST calls between services.
 * 
 * Flow:
 * 1. LobbyStartedEvent (Spring event) → This publisher
 * 2. Publishes to RabbitMQ: game.session.start.requested
 * 3. Game Service consumes event and creates session
 * 4. Game Service publishes: game.session.started
 * 5. Platform Backend consumes event and updates lobby
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionRequestPublisher {

    private final RabbitTemplate rabbitTemplate;
    
    @Value("${game.events.exchange.name:game_events}")
    private String gameEventsExchange;
    
    @Value("${game.events.routing-keys.session-start-requested:game.session.start.requested}")
    private String sessionStartRequestedRoutingKey;
    
    /**
     * Listens to LobbyStartedEvent and publishes game session start request to RabbitMQ.
     * 
     * @param event The lobby started event
     */
    @EventListener
    public void onLobbyStarted(LobbyStartedEvent event) {
        log.info("Publishing game session start request - lobbyId={}, gameId={}, players={}", 
                event.lobbyId(), event.gameId(), event.playerIds());
        
        try {
            // Determine game type from gameId
            String gameType = determineGameType(event.gameId());
            
            // Skip chess games - they are handled by ChessGameLobbyHandler
            if ("chess".equals(gameType)) {
                log.info("Skipping game session request for chess game - handled by ChessGameLobbyHandler");
                return;
            }
            
            // Generate session ID
            UUID sessionId = UUID.randomUUID();
            
            // Get game configuration based on game type
            Map<String, Object> configuration = getGameConfiguration(event.gameId(), gameType);
            
            // Build event message
            Map<String, Object> message = new HashMap<>();
            message.put("lobby_id", event.lobbyId().toString());
            message.put("session_id", sessionId.toString());
            message.put("game_type", gameType);
            message.put("game_id", event.gameId().toString());
            message.put("player_ids", event.playerIds().stream()
                    .map(UUID::toString)
                    .toList());
            message.put("starting_player_id", event.playerIds().isEmpty() 
                    ? null 
                    : event.playerIds().get(0).toString());
            message.put("configuration", configuration);
            message.put("timestamp", java.time.Instant.now().toString());
            
            // Publish to RabbitMQ
            rabbitTemplate.convertAndSend(
                    gameEventsExchange,
                    sessionStartRequestedRoutingKey,
                    message
            );
            
            log.info("Published game session start request - sessionId={}, gameType={}, routingKey={}", 
                    sessionId, gameType, sessionStartRequestedRoutingKey);
            
        } catch (Exception e) {
            log.error("Failed to publish game session start request - lobbyId={}, gameId={}", 
                    event.lobbyId(), event.gameId(), e);
            // Don't throw - event publishing failures shouldn't break lobby start
        }
    }
    
    // Chess game ID from game service
    private static final UUID CHESS_GAME_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    
    /**
     * Determines game type from game ID.
     * 
     * @param gameId The game ID
     * @return The game type (e.g., "connect_four", "chess")
     */
    private String determineGameType(UUID gameId) {
        // Check if this is a chess game
        if (CHESS_GAME_ID.equals(gameId)) {
            return "chess";
        }
        // Default to connect_four for native games
        return "connect_four";
    }
    
    /**
     * Gets game configuration based on game type.
     * 
     * @param gameId The game ID
     * @param gameType The game type
     * @return Game configuration map
     */
    private Map<String, Object> getGameConfiguration(UUID gameId, String gameType) {
        Map<String, Object> config = new HashMap<>();
        
        switch (gameType) {
            case "connect_four":
                config.put("rows", 6);
                config.put("columns", 7);
                break;
            case "chess":
                // Chess configuration would come from external service
                config.put("initialFen", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
                break;
            default:
                log.warn("Unknown game type: {}, using default configuration", gameType);
        }
        
        return config;
    }
}
