package com.banditgames.platform.achievements.domain;

import java.util.UUID;

/**
 * Interface for evaluating achievement criteria.
 * 
 * This interface follows the Strategy pattern to allow different
 * achievement types to be evaluated in an expandable way.
 * 
 * Each game can implement its own evaluators for game-specific achievements.
 */
public interface AchievementEvaluator {
    
    /**
     * Checks if an achievement's criteria are met.
     * 
     * @param achievement The achievement to evaluate
     * @param playerId The player to evaluate for
     * @param statistics The player's statistics
     * @param context Additional context from the game event
     * @return true if the achievement criteria are met, false otherwise
     */
    boolean evaluate(
            Achievement achievement,
            UUID playerId,
            PlayerStatistics statistics,
            EvaluationContext context
    );
    
    /**
     * Returns whether this evaluator can handle the given achievement.
     * 
     * @param achievement The achievement to check
     * @return true if this evaluator can handle the achievement
     */
    boolean canEvaluate(Achievement achievement);
    
    /**
     * Context for achievement evaluation.
     */
    record EvaluationContext(
            UUID gameId,
            String gameType,
            UUID winnerId,
            UUID playerId,
            java.util.Map<String, Object> rawEvent
    ) {}
}

