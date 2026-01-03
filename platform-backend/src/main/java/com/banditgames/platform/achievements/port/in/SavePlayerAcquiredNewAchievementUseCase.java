package com.banditgames.platform.achievements.port.in;

import com.banditgames.platform.achievements.domain.UserAchievement;

import java.util.UUID;

public interface SavePlayerAcquiredNewAchievementUseCase {

    UserAchievement saveNewAchievement(SavePlayerAcquiredNewAchievement record);
    UserAchievement saveNewThirdPartyAchievement(SavePlayerAcquiredNewThirdPartyAchievement record);
    
    /**
     * Checks if a player already has a specific achievement.
     * 
     * @param playerId The player ID
     * @param achievementId The achievement ID
     * @return true if the player has the achievement, false otherwise
     */
    boolean hasAchievement(UUID playerId, UUID achievementId);

    record SavePlayerAcquiredNewAchievement(UUID playerId, UUID achievementId) {}

    record SavePlayerAcquiredNewThirdPartyAchievement(UUID playerId, String achievementCode) {}
}
