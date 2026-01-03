package com.banditgames.platform.acl.adapter;

import com.banditgames.platform.acl.adapter.messaging.chess.*;
import com.banditgames.platform.acl.port.out.GameContextPort;
import com.banditgames.platform.player.port.in.SearchPlayersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Anti-Corruption Layer (ACL) adapter for external Chess game service.
 *
 * This adapter consumes messages from the chess game's RabbitMQ exchange (gameExchange)
 * and transforms them to platform format, republishing to game_events exchange.
 *
 * **Architecture Principle**: Chess game state is managed by external chess service,
 * NOT by game-service. This adapter only transforms events for logging and notifications.
 *
 * Flow:
 * Chess Game → gameExchange → ACL Adapter → game_events exchange (for logging)
 *
 * The chess game publishes to:
 * - Exchange: gameExchange (TopicExchange)
 * - Routing keys: game.created, game.player.names.updated, game.ended,
 *                  game.registered, move.made, achievement.acquired
 *
 * The ACL adapter:
 * - Transforms external chess events to platform format
 * - Republishes to game_events exchange for logging/notifications
 * - NO direct calls to game-service (chess state managed externally)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChessGameACLAdapter {

    private final RabbitTemplate rabbitTemplate;
    private final SearchPlayersUseCase searchPlayersUseCase;
    private final GameContextPort gameContextPort;

    @Value("${game.events.exchange.name:game_events}")
    private String gameServiceExchange;

    @Value("${game.events.routing-keys.session-started:game.session.started}")
    private String sessionStartedRoutingKey;

    @Value("${game.events.routing-keys.move-request:game.move.request}")
    private String moveRequestRoutingKey;

    @Value("${game.events.routing-keys.session-ended:game.session.ended}")
    private String sessionEndedRoutingKey;

    @Value("${game.events.routing-keys.achievement-unlocked:game.achievement.unlocked}")
    private String achievementUnlockedRoutingKey;

    /**
     * Handles chess game created event.
     * Routing key: game.created
     *
     * Transforms chess game created event to platform format and republishes for logging.
     *
     * **Note**: Chess game state is managed by external chess service, NOT by game-service.
     * This adapter only transforms events for logging and notifications.
     * 
     * **Player ID Lookup**: Attempts to find player IDs by name. If lookup fails,
     * the event is still published with player names for logging purposes.
     */
    @RabbitListener(queues = "${chess.game.queues.game-created}")
    public void handleGameCreated(ChessGameCreatedMessage message) {
        log.info("ACL: Received chess game created: gameId={}, white={}, black={}",
                message.getGameId(), message.getWhitePlayer(), message.getBlackPlayer());

        try {
            // Find player IDs from player names (may be null if players don't exist in platform)
            UUID whitePlayerId = findPlayerIdByName(message.getWhitePlayer());
            UUID blackPlayerId = findPlayerIdByName(message.getBlackPlayer());

            // Use player IDs if found, otherwise use empty list (will be logged but event still published)
            List<String> playerIds;
            String startingPlayerId = null;
            
            if (whitePlayerId != null && blackPlayerId != null) {
                playerIds = List.of(whitePlayerId.toString(), blackPlayerId.toString());
                startingPlayerId = whitePlayerId.toString();
            } else {
                // If player lookup failed, use empty list but still publish event for logging
                playerIds = List.of();
                log.warn("ACL: Could not find player IDs for chess game: white={}, black={}. " +
                        "Event will be published with player names only for logging.",
                        message.getWhitePlayer(), message.getBlackPlayer());
            }

            String sessionId = message.getGameId().toString();

            // Transform and republish event for logging/notifications
            // Chess game state is managed by external chess service, not game-service
            Map<String, Object> gameServiceEvent = new HashMap<>();
            gameServiceEvent.put("eventId", UUID.randomUUID().toString());
            gameServiceEvent.put("timestamp", Instant.now().toString());
            gameServiceEvent.put("gameId", sessionId);
            gameServiceEvent.put("sessionId", sessionId);
            gameServiceEvent.put("gameType", "chess");
            gameServiceEvent.put("lobbyId", null);
            gameServiceEvent.put("playerIds", playerIds);
            if (startingPlayerId != null) {
                gameServiceEvent.put("startingPlayerId", startingPlayerId);
            }
            Map<String, Object> gameConfiguration = new HashMap<>();
            gameConfiguration.put("whitePlayer", message.getWhitePlayer() != null ? message.getWhitePlayer() : "");
            gameConfiguration.put("blackPlayer", message.getBlackPlayer() != null ? message.getBlackPlayer() : "");
            gameConfiguration.put("whitePlayerId", whitePlayerId != null ? whitePlayerId.toString() : "unknown");
            gameConfiguration.put("blackPlayerId", blackPlayerId != null ? blackPlayerId.toString() : "unknown");
            gameConfiguration.put("initialFen", message.getCurrentFen() != null ? message.getCurrentFen() : "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            gameConfiguration.put("status", message.getStatus() != null ? message.getStatus() : "");
            gameServiceEvent.put("gameConfiguration", gameConfiguration);
            gameServiceEvent.put("type", "GAME_SESSION_STARTED");

            rabbitTemplate.convertAndSend(
                gameServiceExchange,
                sessionStartedRoutingKey,
                gameServiceEvent
            );

            log.info("ACL: Transformed and republished chess game created event - gameId={}, playerIdsFound={}",
                    message.getGameId(), whitePlayerId != null && blackPlayerId != null);

        } catch (Exception e) {
            log.error("ACL: Error processing chess game created: {}", message, e);
            // Don't throw - ACL should handle errors gracefully
        }
    }

    /**
     * Handles chess game updated event (player names updated).
     * Routing key: game.player.names.updated
     */
    @RabbitListener(queues = "${chess.game.queues.game-updated}")
    public void handleGameUpdated(ChessGameUpdatedMessage message) {
        log.info("ACL: Received chess game updated: gameId={}, updateType={}",
                message.getGameId(), message.getUpdateType());

        try {
            // Player names updated - typically no action needed in platform
            // unless we need to sync player information
            log.debug("ACL: Chess game player names updated - gameId={}, white={}, black={}",
                    message.getGameId(), message.getWhitePlayer(), message.getBlackPlayer());

        } catch (Exception e) {
            log.error("ACL: Error processing chess game updated: {}", message, e);
        }
    }

    /**
     * Handles chess game ended event.
     * Routing key: game.ended
     *
     * Game-service session is already ended (via moves), but we republish event for logging.
     * Game-service manages session lifecycle through move operations.
     * 
     * **Player ID Lookup**: Attempts to find player IDs by name. If lookup fails,
     * the event is still published with player names for logging purposes.
     */
    @RabbitListener(queues = "${chess.game.queues.game-ended}")
    public void handleGameEnded(ChessGameEndedMessage message) {
        log.info("ACL: Received chess game ended: gameId={}, winner={}, reason={}",
                message.getGameId(), message.getWinner(), message.getEndReason());

        try {
            // Find player IDs (may be null if players don't exist in platform)
            UUID whitePlayerId = findPlayerIdByName(message.getWhitePlayer());
            UUID blackPlayerId = findPlayerIdByName(message.getBlackPlayer());

            if (whitePlayerId == null || blackPlayerId == null) {
                log.warn("ACL: Could not find player IDs for ended chess game: white={}, black={}. " +
                        "Event will be published with player names only for logging.",
                        message.getWhitePlayer(), message.getBlackPlayer());
            }

            // Determine winner ID (may be null if player IDs not found)
            String winnerId = null;
            if (whitePlayerId != null && blackPlayerId != null) {
                if ("WHITE".equals(message.getWinner())) {
                    winnerId = whitePlayerId.toString();
                } else if ("BLACK".equals(message.getWinner())) {
                    winnerId = blackPlayerId.toString();
                }
                // If DRAW, winnerId remains null
            }

            // Note: Game-service session is typically ended through the last move
            // We republish event for logging/notifications
            Map<String, Object> gameServiceEvent = new HashMap<>();
            gameServiceEvent.put("eventId", UUID.randomUUID().toString());
            gameServiceEvent.put("timestamp", Instant.now().toString());
            gameServiceEvent.put("gameId", message.getGameId().toString());
            gameServiceEvent.put("sessionId", message.getGameId().toString());
            gameServiceEvent.put("gameType", "chess");
            if (winnerId != null) {
                gameServiceEvent.put("winnerId", winnerId);
            }
            gameServiceEvent.put("gameResult", mapChessEndReasonToGameResult(message.getEndReason(), message.getWinner()));
            Map<String, Object> finalGameState = new HashMap<>();
            finalGameState.put("fen", message.getFinalFen() != null ? message.getFinalFen() : "");
            finalGameState.put("totalMoves", message.getTotalMoves() != null ? message.getTotalMoves() : 0);
            finalGameState.put("endReason", message.getEndReason() != null ? message.getEndReason() : "");
            finalGameState.put("winner", message.getWinner() != null ? message.getWinner() : "");
            finalGameState.put("whitePlayer", message.getWhitePlayer() != null ? message.getWhitePlayer() : "");
            finalGameState.put("blackPlayer", message.getBlackPlayer() != null ? message.getBlackPlayer() : "");
            finalGameState.put("whitePlayerId", whitePlayerId != null ? whitePlayerId.toString() : "unknown");
            finalGameState.put("blackPlayerId", blackPlayerId != null ? blackPlayerId.toString() : "unknown");
            gameServiceEvent.put("finalGameState", finalGameState);
            gameServiceEvent.put("type", "GAME_SESSION_ENDED");

            // Publish to game-service exchange for logging
            rabbitTemplate.convertAndSend(
                gameServiceExchange,
                sessionEndedRoutingKey,
                gameServiceEvent
            );

            log.info("ACL: Transformed and republished chess game ended event - gameId={}, winnerId={}, playerIdsFound={}",
                    message.getGameId(), winnerId, whitePlayerId != null && blackPlayerId != null);

        } catch (Exception e) {
            log.error("ACL: Error processing chess game ended: {}", message, e);
        }
    }

    /**
     * Maps chess end reason to game-service game result format.
     */
    private String mapChessEndReasonToGameResult(String endReason, String winner) {
        if ("CHECKMATE".equals(endReason)) {
            return "WIN";
        } else if ("DRAW".equals(endReason) || "DRAW".equals(winner)) {
            return "DRAW";
        }
        return "FINISHED";
    }

    /**
     * Handles chess move made event.
     * Routing key: move.made
     *
     * Transforms chess move event to platform format and republishes for logging.
     *
     * **Note**: Chess game state is managed by external chess service, NOT by game-service.
     * This adapter only transforms events for logging and notifications.
     * 
     * **Player ID Lookup**: Attempts to find player ID by name. If lookup fails,
     * the event is still published with player name for logging purposes.
     */
    @RabbitListener(queues = "${chess.game.queues.move-made}")
    public void handleMoveMade(ChessMoveMadeMessage message) {
        log.debug("ACL: Received chess move made: gameId={}, move={}, player={}",
                message.getGameId(), message.getSanNotation(), message.getPlayer());

        try {
            // Find player ID from player name/color (may be null if player doesn't exist in platform)
            UUID playerId = null;
            if ("WHITE".equals(message.getPlayer())) {
                playerId = findPlayerIdByName(message.getWhitePlayer());
            } else if ("BLACK".equals(message.getPlayer())) {
                playerId = findPlayerIdByName(message.getBlackPlayer());
            }

            if (playerId == null) {
                log.warn("ACL: Could not find player ID for chess move: player={}, playerName={}. " +
                        "Event will be published with player name only for logging.",
                        message.getPlayer(), 
                        "WHITE".equals(message.getPlayer()) ? message.getWhitePlayer() : message.getBlackPlayer());
            }

            String sessionId = message.getGameId().toString();

            // Transform and republish event for logging/notifications
            // Chess game state is managed by external chess service, not game-service
            Map<String, Object> gameServiceEvent = new HashMap<>();
            gameServiceEvent.put("eventId", UUID.randomUUID().toString());
            gameServiceEvent.put("timestamp", Instant.now().toString());
            gameServiceEvent.put("gameId", sessionId);
            gameServiceEvent.put("sessionId", sessionId);
            gameServiceEvent.put("gameType", "chess");
            if (playerId != null) {
                gameServiceEvent.put("playerId", playerId.toString());
            }
            Map<String, Object> moveData = new HashMap<>();
            moveData.put("fromSquare", message.getFromSquare() != null ? message.getFromSquare() : "");
            moveData.put("toSquare", message.getToSquare() != null ? message.getToSquare() : "");
            moveData.put("sanNotation", message.getSanNotation() != null ? message.getSanNotation() : "");
            moveData.put("fenAfterMove", message.getFenAfterMove() != null ? message.getFenAfterMove() : "");
            moveData.put("player", message.getPlayer() != null ? message.getPlayer() : "");
            moveData.put("playerName", "WHITE".equals(message.getPlayer()) 
                ? (message.getWhitePlayer() != null ? message.getWhitePlayer() : "") 
                : (message.getBlackPlayer() != null ? message.getBlackPlayer() : ""));
            moveData.put("playerId", playerId != null ? playerId.toString() : "unknown");
            moveData.put("moveNumber", message.getMoveNumber() != null ? message.getMoveNumber() : 0);
            gameServiceEvent.put("move", moveData);
            gameServiceEvent.put("type", "GAME_MOVE_REQUEST");

            rabbitTemplate.convertAndSend(
                gameServiceExchange,
                moveRequestRoutingKey,
                gameServiceEvent
            );

            log.debug("ACL: Transformed and republished chess move event - gameId={}, move={}, playerIdFound={}",
                    message.getGameId(), message.getSanNotation(), playerId != null);

        } catch (Exception e) {
            log.error("ACL: Error processing chess move made: {}", message, e);
        }
    }

    /**
     * Handles chess game registered event.
     * Routing key: game.registered
     *
     * Registers chess game achievements in the platform.
     * Note: Achievements are registered when the game is registered, but they are only
     * awarded to players when they actually earn them (via handleAchievementAcquired).
     */
    @RabbitListener(queues = "${chess.game.queues.game-registered}")
    public void handleGameRegistered(ChessGameRegisteredMessage message) {
        log.info("ACL: Received chess game registered: registrationId={}, frontendUrl={}",
                message.getRegistrationId(), message.getFrontendUrl());

        try {
            // Register chess game achievements in platform
            // Achievements are registered as third-party achievements when players earn them
            // (handled in handleAchievementAcquired). We just log them here for reference.
            if (message.getAvailableAchievements() != null) {
                for (ChessGameRegisteredMessage.ChessAchievement achievement : message.getAvailableAchievements()) {
                    log.info("ACL: Chess achievement available - code={}, description={}",
                            achievement.getCode(), achievement.getDescription());
                }
            }

            log.info("ACL: Chess game registered successfully - registrationId={}, achievements={}",
                    message.getRegistrationId(),
                    message.getAvailableAchievements() != null ? message.getAvailableAchievements().size() : 0);

        } catch (Exception e) {
            log.error("ACL: Error processing chess game registered: {}", message, e);
        }
    }

    /**
     * Handles chess achievement acquired event.
     * Routing key: achievement.acquired
     *
     * Awards the achievement to the player via GameContextPort and also
     * transforms to game-service game.achievement.unlocked event for logging.
     */
    @RabbitListener(queues = "${chess.game.queues.achievement-acquired}")
    public void handleAchievementAcquired(ChessAchievementAcquiredMessage message) {
        log.info("ACL: Received chess achievement acquired: gameId={}, playerId={}, achievement={}",
                message.getGameId(), message.getPlayerId(), message.getAchievementType());

        try {
            // Award achievement to player via GameContextPort
            // This registers the achievement (if needed) and awards it to the player
            if (message.getPlayerId() != null && message.getGameId() != null) {
                gameContextPort.handleThirdPartyAchievementUnlocked(
                        message.getGameId(),
                        message.getPlayerId(),
                        message.getAchievementType(),
                        message.getAchievementType(), // Use achievement type as name
                        message.getAchievementDescription() != null 
                                ? message.getAchievementDescription() 
                                : "Chess achievement: " + message.getAchievementType()
                );
                log.info("ACL: Awarded chess achievement to player - playerId={}, achievement={}",
                        message.getPlayerId(), message.getAchievementType());
            } else {
                log.warn("ACL: Cannot award achievement - missing playerId or gameId: playerId={}, gameId={}",
                        message.getPlayerId(), message.getGameId());
            }

            // Transform chess achievement to game-service achievement unlocked event for logging
            Map<String, Object> gameServiceEvent = new HashMap<>();
            gameServiceEvent.put("eventId", UUID.randomUUID().toString());
            gameServiceEvent.put("timestamp", Instant.now().toString());
            gameServiceEvent.put("gameId", message.getGameId().toString());
            gameServiceEvent.put("gameType", "chess");
            gameServiceEvent.put("playerId", message.getPlayerId().toString());
            gameServiceEvent.put("achievementCode", message.getAchievementType());
            gameServiceEvent.put("achievementName", message.getAchievementType());
            gameServiceEvent.put("achievementDescription", message.getAchievementDescription());
            gameServiceEvent.put("type", "ACHIEVEMENT_UNLOCKED");

            // Publish to game-service exchange for logging
            rabbitTemplate.convertAndSend(
                gameServiceExchange,
                achievementUnlockedRoutingKey,
                gameServiceEvent
            );

            log.info("ACL: Transformed and republished chess achievement to game-service - playerId={}, achievement={}",
                    message.getPlayerId(), message.getAchievementType());

        } catch (Exception e) {
            log.error("ACL: Error processing chess achievement acquired: {}", message, e);
        }
    }

    /**
     * Helper method to find player ID by name.
     * Resolves chess player names to platform player IDs.
     * 
     * @param playerName The player name to search for
     * @return The player ID if found, null otherwise
     */
    private UUID findPlayerIdByName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            log.debug("ACL: Player name is null or empty, cannot lookup player ID");
            return null;
        }
        
        try {
            var players = searchPlayersUseCase.searchPlayers(playerName.trim(), null, Pageable.unpaged());
            if (players != null && !players.isEmpty() && !players.getContent().isEmpty()) {
                UUID foundPlayerId = players.getContent().getFirst().getPlayerId();
                log.debug("ACL: Found player ID for name '{}': {}", playerName, foundPlayerId);
                return foundPlayerId;
            } else {
                log.debug("ACL: No player found with name: {}", playerName);
            }
        } catch (Exception e) {
            log.debug("ACL: Error looking up player by name '{}': {}", playerName, e.getMessage());
        }
        return null;
    }
}
