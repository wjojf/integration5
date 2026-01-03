package com.banditgames.platform.achievements.domain.evaluator;

import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.domain.AchievementEvaluator;
import com.banditgames.platform.achievements.domain.PlayerStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluator for social achievements (e.g., "Play against 20 unique players").
 * 
 * This evaluator checks if social interaction criteria are met.
 */
@Slf4j
@Component
public class SocialAchievementEvaluator implements AchievementEvaluator {
    
    private static final Pattern UNIQUE_PLAYERS_PATTERN = Pattern.compile("(?i)(\\d+)\\s+unique\\s+players?");
    
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
        
        Matcher matcher = UNIQUE_PLAYERS_PATTERN.matcher(textToCheck);
        if (matcher.find()) {
            int threshold = Integer.parseInt(matcher.group(1));
            int uniqueOpponents = statistics.getUniqueOpponents() != null 
                    ? statistics.getUniqueOpponents().size() 
                    : 0;
            boolean met = uniqueOpponents >= threshold;
            log.debug("Social achievement evaluation - achievement: {}, player: {}, uniqueOpponents: {}, threshold: {}, met: {}", 
                    achievement.getName(), playerId, uniqueOpponents, threshold, met);
            return met;
        }
        
        log.warn("Social achievement evaluator could not parse threshold from: {}", textToCheck);
        return false;
    }
    
    @Override
    public boolean canEvaluate(Achievement achievement) {
        // Social achievements are typically categorized as SOCIAL but may use COUNTER_REACHES_THRESHOLD
        return achievement.getCategory() != null 
                && achievement.getCategory().name().equals("SOCIAL");
    }
}

