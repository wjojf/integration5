package com.banditgames.platform.achievements.usecase;

import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.domain.AchievementEvaluator;
import com.banditgames.platform.achievements.domain.PlayerStatistics;
import com.banditgames.platform.achievements.port.in.EvaluateAchievementsUseCase;
import com.banditgames.platform.achievements.port.in.SavePlayerAcquiredNewAchievementUseCase;
import com.banditgames.platform.achievements.port.out.LoadAchievementsPort;
import com.banditgames.platform.achievements.port.out.LoadPlayerStatisticsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Service for evaluating achievements based on game events.
 * 
 * This service:
 * 1. Loads relevant achievements for a game
 * 2. Loads player statistics
 * 3. Evaluates each achievement using appropriate evaluators
 * 4. Unlocks achievements when criteria are met
 * 5. Updates player statistics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluateAchievementsService implements EvaluateAchievementsUseCase {
    
    private final LoadAchievementsPort loadAchievementsPort;
    private final LoadPlayerStatisticsPort loadPlayerStatisticsPort;
    private final SavePlayerAcquiredNewAchievementUseCase savePlayerAcquiredNewAchievementUseCase;
    private final List<AchievementEvaluator> achievementEvaluators;
    
    @Override
    public void evaluateOnGameEnded(GameEndedEvaluationContext context) {
        log.info("Evaluating achievements for game ended - gameId: {}, gameType: {}, players: {}", 
                context.gameId(), context.gameType(), context.playerIds().size());
        
        try {
            // Load all achievements for this game type
            // Note: gameId here represents the game type (e.g., connect_four, chess), not a session ID
            // Achievements are defined per game type, not per session
            List<Achievement> achievements = loadAchievementsPort.findByGameId(context.gameId());
            
            if (achievements.isEmpty()) {
                log.debug("No achievements found for game: {}", context.gameId());
                return;
            }
            
            // Calculate game duration if available
            Duration gameDuration = calculateGameDuration(context.rawEvent());
            
            // Evaluate achievements for each player
            for (UUID playerId : context.playerIds()) {
                evaluateAchievementsForPlayer(
                        playerId,
                        context.gameId(),
                        achievements,
                        context,
                        gameDuration
                );
            }
            
        } catch (Exception e) {
            log.error("Error evaluating achievements for game ended: {}", context, e);
        }
    }
    
    @Override
    public void evaluateOnMoveApplied(GameMoveEvaluationContext context) {
        log.debug("Evaluating achievements for move applied - gameId: {}, gameType: {}, playerId: {}", 
                context.gameId(), context.gameType(), context.playerId());
        
        try {
            // Load all achievements for this game type
            // Note: gameId here represents the game type (e.g., connect_four, chess), not a session ID
            // Achievements are defined per game type, not per session
            List<Achievement> achievements = loadAchievementsPort.findByGameId(context.gameId());
            
            if (achievements.isEmpty()) {
                return;
            }
            
            // Evaluate achievements for the player
            evaluateAchievementsForPlayer(
                    context.playerId(),
                    context.gameId(),
                    achievements,
                    context,
                    null
            );
            
        } catch (Exception e) {
            log.error("Error evaluating achievements for move applied: {}", context, e);
        }
    }
    
    private void evaluateAchievementsForPlayer(
            UUID playerId,
            UUID gameId,  // This is the game type ID, not a session ID
            List<Achievement> achievements,
            Object context,
            Duration gameDuration
    ) {
        // Load or initialize player statistics for this game type
        // Statistics are tracked per player per game type (not per session)
        PlayerStatistics statistics = loadPlayerStatisticsPort.loadStatistics(playerId, gameId);
        
        // Update statistics based on context
        if (context instanceof GameEndedEvaluationContext endedContext) {
            updateStatisticsForGameEnded(statistics, endedContext, gameDuration);
        }
        
        // Create evaluation context
        AchievementEvaluator.EvaluationContext evalContext = createEvaluationContext(context, playerId);
        
        // Evaluate each achievement
        for (Achievement achievement : achievements) {
            // Skip third-party achievements (handled separately)
            if (Boolean.TRUE.equals(achievement.getThirdPartyAchievement())) {
                continue;
            }
            
            // Check if player already has this achievement
            if (savePlayerAcquiredNewAchievementUseCase.hasAchievement(playerId, achievement.getId())) {
                continue;
            }
            
            // Find appropriate evaluator
            AchievementEvaluator evaluator = findEvaluator(achievement);
            if (evaluator == null) {
                log.warn("No evaluator found for achievement: {}", achievement.getName());
                continue;
            }
            
            // Evaluate achievement
            boolean criteriaMet = evaluator.evaluate(achievement, playerId, statistics, evalContext);
            
            if (criteriaMet) {
                // Unlock achievement
                unlockAchievement(playerId, achievement);
            }
        }
        
        // Save updated statistics
        loadPlayerStatisticsPort.updateStatistics(playerId, gameId, statistics);
    }
    
    private void updateStatisticsForGameEnded(
            PlayerStatistics statistics,
            GameEndedEvaluationContext context,
            Duration gameDuration
    ) {
        UUID playerId = statistics.getPlayerId();
        
        // Determine if player won, lost, or drew
        boolean isWinner = context.winnerId() != null && context.winnerId().equals(playerId);
        boolean isDraw = context.winnerId() == null;
        
        if (isWinner) {
            statistics.recordWin();
            if (gameDuration != null) {
                statistics.updateFastestWin(gameDuration);
            }
        } else if (isDraw) {
            statistics.recordDraw();
        } else {
            statistics.recordLoss();
        }
        
        // Record opponents
        for (UUID opponentId : context.playerIds()) {
            if (!opponentId.equals(playerId)) {
                statistics.recordOpponent(opponentId);
            }
        }
        
        // Add play time
        if (gameDuration != null) {
            statistics.addPlayTime(gameDuration);
        }
    }
    
    private AchievementEvaluator.EvaluationContext createEvaluationContext(Object context, UUID playerId) {
        if (context instanceof GameEndedEvaluationContext endedContext) {
            return new AchievementEvaluator.EvaluationContext(
                    endedContext.gameId(),
                    endedContext.gameType(),
                    endedContext.winnerId(),
                    playerId,
                    endedContext.rawEvent()
            );
        } else if (context instanceof GameMoveEvaluationContext moveContext) {
            return new AchievementEvaluator.EvaluationContext(
                    moveContext.gameId(),
                    moveContext.gameType(),
                    null,
                    playerId,
                    moveContext.rawEvent()
            );
        }
        return null;
    }
    
    private AchievementEvaluator findEvaluator(Achievement achievement) {
        return achievementEvaluators.stream()
                .filter(evaluator -> evaluator.canEvaluate(achievement))
                .findFirst()
                .orElse(null);
    }
    
    private void unlockAchievement(UUID playerId, Achievement achievement) {
        try {
            var record = new SavePlayerAcquiredNewAchievementUseCase.SavePlayerAcquiredNewAchievement(
                    playerId,
                    achievement.getId()
            );
            
            savePlayerAcquiredNewAchievementUseCase.saveNewAchievement(record);
            
            log.info("Unlocked achievement - player: {}, achievement: {} ({})", 
                    playerId, achievement.getName(), achievement.getId());
        } catch (Exception e) {
            log.error("Error unlocking achievement - player: {}, achievement: {}", 
                    playerId, achievement.getId(), e);
        }
    }
    
    private Duration calculateGameDuration(java.util.Map<String, Object> rawEvent) {
        try {
            String timestampStr = (String) rawEvent.get("timestamp");
            if (timestampStr != null) {
                // For now, we don't have start time, so we can't calculate duration
                // This would need to be enhanced to track game start time
                return null;
            }
        } catch (Exception e) {
            log.debug("Could not calculate game duration: {}", e.getMessage());
        }
        return null;
    }
}

