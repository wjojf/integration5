package com.banditgames.platform.achievements.domain.evaluator;

import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.domain.AchievementCriteria;
import com.banditgames.platform.achievements.domain.AchievementEvaluator;
import com.banditgames.platform.achievements.domain.PlayerStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluator for one-time event achievements (e.g., "First Victory").
 * 
 * This evaluator checks if a specific one-time event has occurred.
 * The event is typically determined by the achievement name or description.
 */
@Slf4j
@Component
public class OneTimeEventAchievementEvaluator implements AchievementEvaluator {
    
    @Override
    public boolean evaluate(
            Achievement achievement,
            UUID playerId,
            PlayerStatistics statistics,
            EvaluationContext context
    ) {
        if (!canEvaluate(achievement)) {
            return false;
        }
        
        String name = achievement.getName().toLowerCase();
        String description = achievement.getDescription().toLowerCase();
        String textToCheck = name + " " + description;
        
        // Check for "first victory" or "first win"
        if (textToCheck.contains("first") && (textToCheck.contains("victory") || textToCheck.contains("win"))) {
            boolean met = statistics.getTotalWins() >= 1;
            log.debug("One-time achievement evaluation - achievement: {}, player: {}, wins: {}, met: {}", 
                    achievement.getName(), playerId, statistics.getTotalWins(), met);
            return met;
        }
        
        // Add more one-time event checks as needed
        log.debug("One-time achievement evaluation - achievement: {}, player: {}, met: false (no matching pattern)", 
                achievement.getName(), playerId);
        return false;
    }
    
    @Override
    public boolean canEvaluate(Achievement achievement) {
        return achievement.getCriteria() == AchievementCriteria.ONE_TIME_EVENT;
    }
}

