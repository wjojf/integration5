package com.banditgames.platform.achievements.port.out;

import com.banditgames.platform.achievements.domain.PlayerStatistics;

import java.util.UUID;

/**
 * Port for loading player statistics needed for achievement evaluation.
 */
public interface LoadPlayerStatisticsPort {
    
    /**
     * Loads statistics for a player in a specific game.
     * 
     * @param playerId The player ID
     * @param gameId The game ID
     * @return Player statistics for the game
     */
    PlayerStatistics loadStatistics(UUID playerId, UUID gameId);
    
    /**
     * Updates player statistics after a game event.
     * 
     * @param playerId The player ID
     * @param gameId The game ID
     * @param statistics The updated statistics
     */
    void updateStatistics(UUID playerId, UUID gameId, PlayerStatistics statistics);
}

