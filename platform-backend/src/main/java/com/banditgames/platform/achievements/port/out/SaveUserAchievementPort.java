package com.banditgames.platform.achievements.port.out;

import com.banditgames.platform.achievements.domain.UserAchievement;

import java.util.UUID;

/**
 * Port for saving user achievements.
 */
public interface SaveUserAchievementPort {
    
    /**
     * Saves a user achievement.
     * 
     * @param userAchievement The user achievement to save
     * @return The saved user achievement
     */
    UserAchievement save(UserAchievement userAchievement);
    
    /**
     * Checks if a player already has a specific achievement.
     * 
     * @param playerId The player ID
     * @param achievementId The achievement ID
     * @return true if the player has the achievement, false otherwise
     */
    boolean existsByPlayerIdAndAchievementId(UUID playerId, UUID achievementId);
}

