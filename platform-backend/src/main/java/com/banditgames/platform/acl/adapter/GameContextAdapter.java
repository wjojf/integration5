package com.banditgames.platform.acl.adapter;

import com.banditgames.platform.acl.port.out.GameContextPort;
import com.banditgames.platform.achievements.port.in.SaveNewThirdPartyAchievementUseCase;
import com.banditgames.platform.achievements.port.in.SavePlayerAcquiredNewAchievementUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Anti-Corruption Layer (ACL) adapter for Game context.
 * 
 * This adapter translates between external Game context events
 * and consuming contexts' domain models, ensuring loose coupling.
 * 
 * The ACL pattern protects consuming contexts from changes in the Game
 * context's internal structure and external message formats.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameContextAdapter implements GameContextPort {
    
    private final SavePlayerAcquiredNewAchievementUseCase savePlayerAcquiredNewAchievementUseCase;
    private final SaveNewThirdPartyAchievementUseCase saveNewThirdPartyAchievementUseCase;
    
    @Override
    public void handleGameEnded(UUID lobbyId, UUID winnerId, List<UUID> playerIds) {
        log.debug("ACL: Handling game ended event for lobby: {}, winner: {}", lobbyId, winnerId);
        
        // Translate game ended event into consuming context actions
        // For now, we could check for win/loss achievements, but that would require
        // querying achievements by trigger type. This is a placeholder for the ACL pattern.
        
        // Example: If we had achievements triggered by game wins/losses,
        // we would check and unlock them here through the use case
        
        log.info("Game ended for lobby {} - winner: {}, participants: {}", 
                lobbyId, winnerId, playerIds);
    }
    
    @Override
    public void handleThirdPartyAchievementUnlocked(
            UUID gameId,
            UUID playerId,
            String achievementCode,
            String achievementName,
            String achievementDescription
    ) {
        log.debug("ACL: Handling third-party achievement unlocked - game: {}, player: {}, code: {}", 
                gameId, playerId, achievementCode);
        
        try {
            // First, ensure the achievement exists in our system
            // If it doesn't exist, create it as a third-party achievement
            var achievementRecord = new SaveNewThirdPartyAchievementUseCase.SaveNewThirdPartyAchievementRecord(
                    gameId,
                    achievementName,
                    achievementDescription,
                    "Third-party achievement from game service",
                    true,
                    achievementCode
            );
            
            // Save or get the achievement (idempotent - creates if doesn't exist)
            saveNewThirdPartyAchievementUseCase.SaveNewThirdPartyAchievement(achievementRecord);
            
            // Then, record that the player unlocked it
            var playerAchievementRecord = 
                    new SavePlayerAcquiredNewAchievementUseCase.SavePlayerAcquiredNewThirdPartyAchievement(
                            playerId,
                            achievementCode
                    );
            
            savePlayerAcquiredNewAchievementUseCase.saveNewThirdPartyAchievement(playerAchievementRecord);
            
            log.info("Successfully processed third-party achievement unlock - player: {}, achievement: {}", 
                    playerId, achievementCode);
        } catch (Exception e) {
            log.error("Error handling third-party achievement unlock - game: {}, player: {}, code: {}", 
                    gameId, playerId, achievementCode, e);
            // Don't throw - ACL should handle errors gracefully
        }
    }
}

