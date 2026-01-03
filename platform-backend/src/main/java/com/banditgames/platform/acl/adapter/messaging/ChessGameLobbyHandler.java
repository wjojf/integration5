package com.banditgames.platform.acl.adapter.messaging;

import com.banditgames.platform.lobby.domain.events.LobbyStartedEvent;
import com.banditgames.platform.lobby.service.ExternalGameInstanceService;
import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles chess game creation when a lobby is started for a chess game.
 * 
 * Flow:
 * 1. LobbyStartedEvent is published
 * 2. This handler checks if it's a chess game
 * 3. If chess: Registers game, creates chess game with both players, publishes event
 * 4. Frontend receives event and redirects both players to same chess game
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChessGameLobbyHandler {

    private final LoadPlayerPort loadPlayerPort;
    private final ExternalGameInstanceService externalGameInstanceService;
    private final RestTemplate restTemplate;
    
    // Chess game ID from game service (hardcoded for now)
    private static final UUID CHESS_GAME_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    
    @Value("${chess.game.service.url:http://localhost:8080}")
    private String chessGameServiceUrl;
    
    @Value("${chess.game.frontend.url:http://localhost:3333}")
    private String chessFrontendUrl;
    
    @Value("${game.events.exchange.name:game_events}")
    private String gameEventsExchange;
    
    /**
     * Listens to LobbyStartedEvent and handles chess game creation if applicable.
     */
    @EventListener
    public void onLobbyStarted(LobbyStartedEvent event) {
        // Check if this is a chess game
        if (!isChessGame(event.gameId())) {
            log.debug("Lobby started for non-chess game: gameId={}, skipping chess handler", event.gameId());
            return;
        }
        
        log.info("Chess lobby started - lobbyId={}, gameId={}, players={}", 
                event.lobbyId(), event.gameId(), event.playerIds());
        
        try {
            // Validate we have exactly 2 players for chess
            if (event.playerIds().size() != 2) {
                log.warn("Chess game requires exactly 2 players, but lobby has {} players", event.playerIds().size());
                return;
            }
            
            UUID player1Id = event.playerIds().get(0);
            UUID player2Id = event.playerIds().get(1);
            
            // Load player information
            Player player1 = loadPlayerPort.findById(player1Id)
                    .orElseThrow(() -> new RuntimeException("Player 1 not found: " + player1Id));
            Player player2 = loadPlayerPort.findById(player2Id)
                    .orElseThrow(() -> new RuntimeException("Player 2 not found: " + player2Id));
            
            // Generate chess game ID (this will be the actual chess game instance ID)
            UUID chessGameId = UUID.randomUUID();
            
            // 1. Register chess game with platform
            registerChessGame(chessGameId, event.lobbyId());
            
            // 2. Create chess game in chess backend with both players
            createChessGameInBackend(chessGameId, player1, player2);
            
            // 3. Store external game instance mapping for frontend to retrieve
            externalGameInstanceService.storeExternalGameInstance(
                    event.lobbyId(), 
                    event.gameId(), 
                    "chess", 
                    chessGameId
            );
            
            log.info("Chess game created successfully - lobbyId={}, chessGameId={}, players=[{}, {}]",
                    event.lobbyId(), chessGameId, player1.getUsername(), player2.getUsername());
            
        } catch (Exception e) {
            log.error("Failed to handle chess lobby start - lobbyId={}, gameId={}", 
                    event.lobbyId(), event.gameId(), e);
            // Don't throw - allow lobby to start even if chess game creation fails
        }
    }
    
    /**
     * Checks if the game ID corresponds to a chess game.
     */
    private boolean isChessGame(UUID gameId) {
        // Chess game ID from game service
        return CHESS_GAME_ID.equals(gameId);
    }
    
    /**
     * Registers the chess game with the platform.
     */
    private void registerChessGame(UUID chessGameId, UUID lobbyId) {
        try {
            String frontendUrl = chessFrontendUrl + "/game/" + chessGameId;
            
            // Call platform backend's chess registration endpoint
            // This is an internal call, so we use the service URL directly
            String registerUrl = "http://platform-backend:8081/api/external/chess/register/" + chessGameId;
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("frontendUrl", frontendUrl);
            requestBody.put("pictureUrl", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSYigKOHenUTuER6t1jBye1G_D1q8IuOauFSQ&s");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    registerUrl,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("Registered chess game with platform - chessGameId={}, response={}", 
                    chessGameId, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Failed to register chess game with platform - chessGameId={}", chessGameId, e);
            throw e;
        }
    }
    
    /**
     * Creates the chess game in the chess backend with both players.
     */
    private void createChessGameInBackend(UUID chessGameId, Player player1, Player player2) {
        try {
            String createGameUrl = chessGameServiceUrl + "/api/games/" + chessGameId;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("whitePlayerId", player1.getPlayerId().toString());
            requestBody.put("whitePlayerName", player1.getUsername());
            requestBody.put("blackPlayerId", player2.getPlayerId().toString());
            requestBody.put("blackPlayerName", player2.getUsername());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    createGameUrl,
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("Created chess game in backend - chessGameId={}, response={}", 
                    chessGameId, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Failed to create chess game in backend - chessGameId={}", chessGameId, e);
            throw e;
        }
    }
    
}

