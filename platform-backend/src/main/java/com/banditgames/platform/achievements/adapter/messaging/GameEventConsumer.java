package com.banditgames.platform.achievements.adapter.messaging;

import com.banditgames.platform.achievements.port.in.EvaluateAchievementsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Event consumer for game events that trigger achievement evaluation.
 * 
 * This consumer listens to game events from RabbitMQ and triggers
 * achievement evaluation when criteria might be met.
 * 
 * Architecture:
 * - Consumes game.session.ended (via fanout) and game.move.applied events
 * - Delegates to achievement evaluation service
 * - Follows EDA pattern for loose coupling
 * - Uses fanout exchange for session ended to ensure all consumers receive message
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventConsumer {

    private final EvaluateAchievementsUseCase evaluateAchievementsUseCase;

    /**
     * Consumes game.session.ended events from RabbitMQ.
     * Triggers achievement evaluation for all players in the game.
     * Uses the achievements-specific queue bound to the fanout exchange to ensure
     * this consumer always receives the message (no competing consumers).
     */
    @RabbitListener(queues = "${game.events.queues.session-ended-achievements}")
    public void onGameSessionEnded(Map<String, Object> event) {
        try {
            log.debug("Received game.session.ended event: {}", event);
            
            String gameIdStr = (String) event.get("game_id");
            String gameType = (String) event.get("game_type");
            String winnerIdStr = (String) event.get("winner_id");
            String sessionIdStr = (String) event.get("session_id");
            
            if (gameIdStr == null || gameType == null) {
                log.warn("Received game.session.ended event without game_id or game_type: {}", event);
                return;
            }
            
            UUID gameId = UUID.fromString(gameIdStr);
            UUID winnerId = winnerIdStr != null ? UUID.fromString(winnerIdStr) : null;
            
            // Extract player IDs from event
            // The event should contain player_ids array or we need to extract from game_state
            @SuppressWarnings("unchecked")
            java.util.List<String> playerIdsList = (java.util.List<String>) event.get("player_ids");
            
            if (playerIdsList == null || playerIdsList.isEmpty()) {
                log.warn("Received game.session.ended event without player_ids: {}", event);
                return;
            }
            
            // Convert player IDs to UUIDs
            java.util.List<UUID> playerIds = playerIdsList.stream()
                    .map(UUID::fromString)
                    .toList();
            
            // Create evaluation context
            var evaluationContext = new EvaluateAchievementsUseCase.GameEndedEvaluationContext(
                    gameId,
                    gameType,
                    sessionIdStr,
                    winnerId,
                    playerIds,
                    event
            );
            
            // Evaluate achievements for all players
            evaluateAchievementsUseCase.evaluateOnGameEnded(evaluationContext);
            
            log.info("Processed game.session.ended event - gameId: {}, gameType: {}, winner: {}, players: {}", 
                    gameId, gameType, winnerId, playerIds.size());
            
        } catch (Exception e) {
            log.error("Error processing game.session.ended event: {}", event, e);
            // Don't throw - event processing failures shouldn't break the system
        }
    }

    /**
     * Consumes game.move.applied events from RabbitMQ.
     * Can trigger achievement evaluation for move-based achievements.
     */
    @RabbitListener(queues = "${game.events.queues.move-applied:game.move.applied}")
    public void onGameMoveApplied(Map<String, Object> event) {
        try {
            log.debug("Received game.move.applied event: {}", event);
            
            String gameIdStr = (String) event.get("game_id");
            String gameType = (String) event.get("game_type");
            String playerIdStr = (String) event.get("player_id");
            
            if (gameIdStr == null || gameType == null || playerIdStr == null) {
                log.warn("Received game.move.applied event without required fields: {}", event);
                return;
            }
            
            UUID gameId = UUID.fromString(gameIdStr);
            UUID playerId = UUID.fromString(playerIdStr);
            
            // Create evaluation context
            var evaluationContext = new EvaluateAchievementsUseCase.GameMoveEvaluationContext(
                    gameId,
                    gameType,
                    playerId,
                    event
            );
            
            // Evaluate achievements for the player who made the move
            evaluateAchievementsUseCase.evaluateOnMoveApplied(evaluationContext);
            
            log.debug("Processed game.move.applied event - gameId: {}, gameType: {}, playerId: {}", 
                    gameId, gameType, playerId);
            
        } catch (Exception e) {
            log.error("Error processing game.move.applied event: {}", event, e);
            // Don't throw - event processing failures shouldn't break the system
        }
    }
}

