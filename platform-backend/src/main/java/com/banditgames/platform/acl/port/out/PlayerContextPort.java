package com.banditgames.platform.acl.port.out;

import com.banditgames.platform.acl.adapter.dto.PlayerInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for accessing Player context information through ACL.
 * This is part of the Anti-Corruption Layer (ACL) pattern to decouple
 * consuming contexts from the Player context.
 * 
 * The ACL translates Player domain concepts into consuming context's language,
 * preventing direct coupling between bounded contexts.
 */
public interface PlayerContextPort {
    
    /**
     * Searches for players by username.
     * Returns only the information needed by consuming contexts (player IDs).
     * 
     * @param username Username to search for (partial match, case-insensitive)
     * @return List of player IDs matching the username
     */
    List<UUID> findPlayerIdsByUsername(String username);
    
    /**
     * Checks if a player exists.
     * 
     * @param playerId Player ID to check
     * @return true if player exists, false otherwise
     */
    boolean playerExists(UUID playerId);
    
    /**
     * Gets public player information by player ID.
     * Returns only public information (excludes private fields like email and address).
     * 
     * @param playerId Player ID to retrieve
     * @return Optional containing PlayerInfo if player exists, empty otherwise
     */
    Optional<PlayerInfo> getPlayerInfo(UUID playerId);
    
    /**
     * Gets public player information for multiple players by their IDs.
     * Returns only public information (excludes private fields like email and address).
     * Only returns information for players that exist.
     * 
     * @param playerIds List of player IDs to retrieve
     * @return List of PlayerInfo for existing players
     */
    List<PlayerInfo> getPlayerInfos(List<UUID> playerIds);
}

