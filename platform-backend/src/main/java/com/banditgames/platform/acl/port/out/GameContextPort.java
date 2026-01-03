package com.banditgames.platform.acl.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Port for accessing Game context information through ACL.
 * This is part of the Anti-Corruption Layer (ACL) pattern to decouple
 * consuming contexts from the Game context.
 * 
 * The ACL translates Game context events into consuming context's language,
 * preventing direct coupling between bounded contexts.
 */
public interface GameContextPort {
    
    /**
     * Handles a game ended event from the Game context.
     * Translates the external event into consuming context actions.
     * 
     * @param lobbyId The lobby ID where the game was played
     * @param winnerId The player ID who won the game (null if no winner)
     * @param playerIds All player IDs who participated in the game
     */
    void handleGameEnded(UUID lobbyId, UUID winnerId, List<UUID> playerIds);
    
    /**
     * Handles a third-party achievement unlocked event from external game service.
     * Translates the external message format into consuming context format.
     * 
     * @param gameId The game ID
     * @param playerId The player who unlocked the achievement
     * @param achievementCode The achievement code from the third-party system
     * @param achievementName The achievement name
     * @param achievementDescription The achievement description
     */
    void handleThirdPartyAchievementUnlocked(
            UUID gameId,
            UUID playerId,
            String achievementCode,
            String achievementName,
            String achievementDescription
    );
}

