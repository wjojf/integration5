package com.banditgames.platform.acl.adapter.messaging;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.events.LobbyStartedEvent;
import com.banditgames.platform.lobby.domain.events.PlayerJoinedLobbyEvent;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.service.ExternalGameInstanceService;
import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles chess game creation and player preregistration for chess lobbies.
 * 
 * Flow:
 * 1. When second player joins chess lobby: Preregister players via PUT /api/games/{gameId}
 * 2. When lobby starts: Use existing chess game, publish session start event
 * 3. Frontend receives event and redirects both players to same chess game
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChessGameLobbyHandler {

    private final LoadPlayerPort loadPlayerPort;
    private final LoadLobbyPort loadLobbyPort;
    private final ExternalGameInstanceService externalGameInstanceService;
    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;
    
    // Chess game ID from game service (hardcoded for now)
    private static final UUID CHESS_GAME_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    
    @Value("${chess.game.service.url:http://localhost:8080}")
    private String chessGameServiceUrl;
    
    @Value("${chess.game.frontend.url:http://localhost:3333}")
    private String chessFrontendUrl;
    
    @Value("${game.events.exchange.name:game_events}")
    private String gameEventsExchange;
    
    /**
     * Listens to PlayerJoinedLobbyEvent and preregisters players for chess games when lobby becomes full.
     * Only for chess games!
     */
    @EventListener
    public void onPlayerJoinedLobby(PlayerJoinedLobbyEvent event) {
        try {
            Lobby lobby = loadLobbyPort.findById(event.lobbyId())
                    .orElse(null);
            
            if (lobby == null || lobby.getGameId() == null) {
                return; // Lobby not found or no game selected yet
            }
            
            // Only handle chess games
            if (!isChessGame(lobby.getGameId())) {
                return;
            }
            
            // Check if lobby is now full (2 players for chess)
            if (lobby.getPlayerIds().size() == 2 && lobby.getStatus().name().equals("WAITING")) {
                log.info("Chess lobby is full - preregistering players - lobbyId={}, players={}", 
                        event.lobbyId(), lobby.getPlayerIds());
                
                UUID player1Id = lobby.getPlayerIds().get(0);
                UUID player2Id = lobby.getPlayerIds().get(1);
                
                // Load player information
                Player player1 = loadPlayerPort.findById(player1Id)
                        .orElseThrow(() -> new RuntimeException("Player 1 not found: " + player1Id));
                Player player2 = loadPlayerPort.findById(player2Id)
                        .orElseThrow(() -> new RuntimeException("Player 2 not found: " + player2Id));
                
                // Generate chess game ID (this will be the actual chess game instance ID)
                UUID chessGameId = UUID.randomUUID();
                
                // 1. Register chess game with platform
                registerChessGame(chessGameId, event.lobbyId());
                
                // 2. Preregister players in chess backend using PUT
                preregisterPlayersInChessBackend(chessGameId, player1, player2);
                
                // 3. Store external game instance mapping for frontend to retrieve
                externalGameInstanceService.storeExternalGameInstance(
                        event.lobbyId(), 
                        lobby.getGameId(), 
                        "chess", 
                        chessGameId
                );
                
                log.info("Preregistered chess players - lobbyId={}, chessGameId={}, players=[{}, {}]",
                        event.lobbyId(), chessGameId, player1.getUsername(), player2.getUsername());
            }
            
        } catch (Exception e) {
            log.error("Failed to preregister chess players - lobbyId={}", event.lobbyId(), e);
            // Don't throw - allow player to join even if preregistration fails
        }
    }
    
    /**
     * Listens to LobbyStartedEvent and handles chess game session creation if applicable.
     * Uses the preregistered chess game if it exists.
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
            
            // Check if chess game was already preregistered
            UUID chessGameId = externalGameInstanceService.getExternalGameInstanceId(event.lobbyId());
            
            if (chessGameId == null) {
                // Chess game wasn't preregistered, create it now
                chessGameId = UUID.randomUUID();
                
                // 1. Register chess game with platform
                registerChessGame(chessGameId, event.lobbyId());
                
                // 2. Store external game instance mapping
                externalGameInstanceService.storeExternalGameInstance(
                        event.lobbyId(), 
                        event.gameId(), 
                        "chess", 
                        chessGameId
                );
            } else {
                log.info("Using preregistered chess game - lobbyId={}, chessGameId={}", 
                        event.lobbyId(), chessGameId);
            }
            
            // Always create/activate the chess game in backend when lobby starts
            // This ensures the game has the correct player IDs and names
            createChessGameInBackend(chessGameId, player1, player2);
            
            // 4. Publish game.session.start.requested event to create session in game-service
            publishGameSessionStartRequested(chessGameId, event.lobbyId(), event.gameId(), player1, player2);
            
            log.info("Chess game session started - lobbyId={}, chessGameId={}, players=[{}, {}]",
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
     * Publishes game.session.start.requested event to trigger session creation in game-service.
     */
    private void publishGameSessionStartRequested(UUID chessGameId, UUID lobbyId, UUID gameId, Player player1, Player player2) {
        try {
            Map<String, Object> gameServiceEvent = new HashMap<>();
            gameServiceEvent.put("eventId", UUID.randomUUID().toString());
            gameServiceEvent.put("timestamp", Instant.now().toString());
            gameServiceEvent.put("session_id", chessGameId.toString());
            gameServiceEvent.put("game_id", gameId.toString());
            gameServiceEvent.put("game_type", "chess");
            gameServiceEvent.put("lobby_id", lobbyId != null ? lobbyId.toString() : null);
            List<String> playerIds = new ArrayList<>();
            playerIds.add(player1.getPlayerId().toString());
            playerIds.add(player2.getPlayerId().toString());
            gameServiceEvent.put("player_ids", playerIds);
            gameServiceEvent.put("starting_player_id", player1.getPlayerId().toString());
            
            Map<String, Object> gameConfiguration = new HashMap<>();
            gameConfiguration.put("whitePlayer", player1.getUsername());
            gameConfiguration.put("blackPlayer", player2.getUsername());
            gameConfiguration.put("whitePlayerId", player1.getPlayerId().toString());
            gameConfiguration.put("blackPlayerId", player2.getPlayerId().toString());
            gameConfiguration.put("initialFen", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            gameConfiguration.put("status", "ACTIVE");
            gameConfiguration.put("externalGameId", chessGameId.toString()); // Store external chess game ID
            gameServiceEvent.put("configuration", gameConfiguration);
            gameServiceEvent.put("type", "GAME_SESSION_START_REQUESTED");
            
            rabbitTemplate.convertAndSend(
                    gameEventsExchange,
                    "game.session.start.requested",
                    gameServiceEvent
            );
            
            log.info("Published game.session.start.requested event - chessGameId={}, lobbyId={}, players=[{}, {}]",
                    chessGameId, lobbyId, player1.getUsername(), player2.getUsername());
            
        } catch (Exception e) {
            log.error("Failed to publish game.session.start.requested event - chessGameId={}", chessGameId, e);
            // Don't throw - game creation should still succeed
        }
    }
    
    /**
     * Preregisters players in chess backend using PUT /api/games/{gameId}.
     * Called when lobby becomes full (2 players) but before game starts.
     */
    private void preregisterPlayersInChessBackend(UUID chessGameId, Player player1, Player player2) {
        try {
            String preregisterUrl = chessGameServiceUrl + "/api/games/" + chessGameId;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("whitePlayerId", player1.getPlayerId().toString());
            requestBody.put("whitePlayerName", player1.getUsername());
            requestBody.put("blackPlayerId", player2.getPlayerId().toString());
            requestBody.put("blackPlayerName", player2.getUsername());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    preregisterUrl,
                    HttpMethod.PUT,
                    request,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            log.info("Preregistered chess players in backend - chessGameId={}, response={}", 
                    chessGameId, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Failed to preregister chess players in backend - chessGameId={}", chessGameId, e);
            throw e;
        }
    }
    
    /**
     * Creates/activates the chess game in the chess backend with both players.
     * Always called when lobby starts to ensure the game has the correct player IDs and names.
     * Sends POST /api/games/{gameId} with player information.
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

