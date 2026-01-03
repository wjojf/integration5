package com.banditgames.platform.achievements.domain.evaluator;

import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.domain.AchievementCriteria;
import com.banditgames.platform.achievements.domain.AchievementEvaluator;
import com.banditgames.platform.achievements.domain.PlayerStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluator for streak-based achievements (e.g., "Win 10 games in a row").
 * 
 * This evaluator checks if a win streak has reached a threshold.
 */
@Slf4j
@Component
public class StreakAchievementEvaluator implements AchievementEvaluator {
    
    private static final Pattern STREAK_PATTERN = Pattern.compile("(?i)(\\d+)\\s+games?\\s+in\\s+a\\s+row");
    
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
        
        String name = achievement.getName();
        String description = achievement.getDescription();
        String textToCheck = (name + " " + description).toLowerCase();
        
        Matcher matcher = STREAK_PATTERN.matcher(textToCheck);
        if (matcher.find()) {
            int threshold = Integer.parseInt(matcher.group(1));
            boolean met = statistics.getCurrentWinStreak() >= threshold;
            log.debug("Streak achievement evaluation - achievement: {}, player: {}, streak: {}, threshold: {}, met: {}", 
                    achievement.getName(), playerId, statistics.getCurrentWinStreak(), threshold, met);
            return met;
        }
        
        log.warn("Streak achievement evaluator could not parse threshold from: {}", textToCheck);
        return false;
    }
    
    @Override
    public boolean canEvaluate(Achievement achievement) {
        return achievement.getCriteria() == AchievementCriteria.STREAK;
    }
}

