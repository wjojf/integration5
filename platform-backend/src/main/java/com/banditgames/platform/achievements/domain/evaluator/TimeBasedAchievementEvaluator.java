package com.banditgames.platform.achievements.domain.evaluator;

import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.domain.AchievementCriteria;
import com.banditgames.platform.achievements.domain.AchievementEvaluator;
import com.banditgames.platform.achievements.domain.PlayerStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluator for time-based achievements (e.g., "Win a game under 2 minutes", "Play for 1 hour").
 * 
 * This evaluator checks if time-based criteria are met.
 */
@Slf4j
@Component
public class TimeBasedAchievementEvaluator implements AchievementEvaluator {
    
    private static final Pattern TIME_UNDER_PATTERN = Pattern.compile("(?i)under\\s+(\\d+)\\s+minutes?");
    private static final Pattern TIME_ACCUMULATED_PATTERN = Pattern.compile("(?i)(\\d+)\\s+hours?\\s+of\\s+play");
    
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
        
        // Check for "win under X minutes"
        Matcher underMatcher = TIME_UNDER_PATTERN.matcher(textToCheck);
        if (underMatcher.find() && textToCheck.contains("win")) {
            int minutesThreshold = Integer.parseInt(underMatcher.group(1));
            Duration threshold = Duration.ofMinutes(minutesThreshold);
            
            if (statistics.getFastestWin() != null) {
                boolean met = statistics.getFastestWin().compareTo(threshold) <= 0;
                log.debug("Time-based achievement evaluation - achievement: {}, player: {}, fastestWin: {}, threshold: {}, met: {}", 
                        achievement.getName(), playerId, statistics.getFastestWin(), threshold, met);
                return met;
            }
            return false;
        }
        
        // Check for accumulated play time
        Matcher accumulatedMatcher = TIME_ACCUMULATED_PATTERN.matcher(textToCheck);
        if (accumulatedMatcher.find()) {
            int hoursThreshold = Integer.parseInt(accumulatedMatcher.group(1));
            Duration threshold = Duration.ofHours(hoursThreshold);
            
            if (statistics.getTotalPlayTime() != null) {
                boolean met = statistics.getTotalPlayTime().compareTo(threshold) >= 0;
                log.debug("Time-based achievement evaluation - achievement: {}, player: {}, totalPlayTime: {}, threshold: {}, met: {}", 
                        achievement.getName(), playerId, statistics.getTotalPlayTime(), threshold, met);
                return met;
            }
            return false;
        }
        
        log.warn("Time-based achievement evaluator could not parse time from: {}", textToCheck);
        return false;
    }
    
    @Override
    public boolean canEvaluate(Achievement achievement) {
        return achievement.getCriteria() == AchievementCriteria.TIME_REACHED;
    }
}

