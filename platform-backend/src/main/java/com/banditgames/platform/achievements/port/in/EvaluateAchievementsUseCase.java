package com.banditgames.platform.achievements.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use case for evaluating achievements based on game events.
 * 
 * This use case evaluates achievement criteria when game events occur
 * and unlocks achievements when criteria are met.
 */
public interface EvaluateAchievementsUseCase {
    
    /**
     * Evaluates achievements when a game ends.
     * 
     * @param context The game ended evaluation context
     */
    void evaluateOnGameEnded(GameEndedEvaluationContext context);
    
    /**
     * Evaluates achievements when a move is applied.
     * 
     * @param context The game move evaluation context
     */
    void evaluateOnMoveApplied(GameMoveEvaluationContext context);
    
    /**
     * Context for game ended event evaluation.
     */
    record GameEndedEvaluationContext(
            UUID gameId,
            String gameType,
            String sessionId,
            UUID winnerId,
            List<UUID> playerIds,
            Map<String, Object> rawEvent
    ) {}
    
    /**
     * Context for game move event evaluation.
     */
    record GameMoveEvaluationContext(
            UUID gameId,
            String gameType,
            UUID playerId,
            Map<String, Object> rawEvent
    ) {}
}

