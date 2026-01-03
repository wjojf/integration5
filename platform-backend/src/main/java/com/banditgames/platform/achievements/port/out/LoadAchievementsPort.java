package com.banditgames.platform.achievements.port.out;

import com.banditgames.platform.achievements.domain.Achievement;

import java.util.List;
import java.util.UUID;

/**
 * Port for loading achievements from persistence.
 */
public interface LoadAchievementsPort {
    
    /**
     * Finds all achievements for a specific game.
     * 
     * @param gameId The game ID
     * @return List of achievements for the game
     */
    List<Achievement> findByGameId(UUID gameId);
}

