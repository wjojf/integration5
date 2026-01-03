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
 * Evaluator for counter-based achievements (e.g., "Win 10 games").
 * 
 * This evaluator checks if a counter (wins, losses, games played, etc.)
 * has reached a threshold defined in the achievement.
 * 
 * The achievement name or description should contain the threshold and metric.
 * Example: "Win 10 games" -> checks if totalWins >= 10
 */
@Slf4j
@Component
public class CounterAchievementEvaluator implements AchievementEvaluator {
    
    private static final Pattern WIN_PATTERN = Pattern.compile("(?i)win\\s+(\\d+)\\s+games?");
    private static final Pattern LOSS_PATTERN = Pattern.compile("(?i)lose\\s+(\\d+)\\s+games?");
    private static final Pattern GAME_PATTERN = Pattern.compile("(?i)play\\s+(\\d+)\\s+games?");
    
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
        
        // Check for win threshold
        Matcher winMatcher = WIN_PATTERN.matcher(textToCheck);
        if (winMatcher.find()) {
            int threshold = Integer.parseInt(winMatcher.group(1));
            boolean met = statistics.getTotalWins() >= threshold;
            log.debug("Counter achievement evaluation - achievement: {}, player: {}, wins: {}, threshold: {}, met: {}", 
                    achievement.getName(), playerId, statistics.getTotalWins(), threshold, met);
            return met;
        }
        
        // Check for loss threshold
        Matcher lossMatcher = LOSS_PATTERN.matcher(textToCheck);
        if (lossMatcher.find()) {
            int threshold = Integer.parseInt(lossMatcher.group(1));
            boolean met = statistics.getTotalLosses() >= threshold;
            log.debug("Counter achievement evaluation - achievement: {}, player: {}, losses: {}, threshold: {}, met: {}", 
                    achievement.getName(), playerId, statistics.getTotalLosses(), threshold, met);
            return met;
        }
        
        // Check for total games threshold
        Matcher gameMatcher = GAME_PATTERN.matcher(textToCheck);
        if (gameMatcher.find()) {
            int threshold = Integer.parseInt(gameMatcher.group(1));
            boolean met = statistics.getTotalGames() >= threshold;
            log.debug("Counter achievement evaluation - achievement: {}, player: {}, games: {}, threshold: {}, met: {}", 
                    achievement.getName(), playerId, statistics.getTotalGames(), threshold, met);
            return met;
        }
        
        log.warn("Counter achievement evaluator could not parse threshold from: {}", textToCheck);
        return false;
    }
    
    @Override
    public boolean canEvaluate(Achievement achievement) {
        return achievement.getCriteria() == AchievementCriteria.COUNTER_REACHES_THRESHOLD;
    }
}

