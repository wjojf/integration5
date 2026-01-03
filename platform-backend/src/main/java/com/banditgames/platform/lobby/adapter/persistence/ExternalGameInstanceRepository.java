package com.banditgames.platform.lobby.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalGameInstanceRepository extends JpaRepository<ExternalGameInstanceEntity, UUID> {
    
    /**
     * Finds external game instance by lobby ID.
     */
    Optional<ExternalGameInstanceEntity> findByLobbyId(UUID lobbyId);
    
    /**
     * Finds external game instance by lobby ID and game type.
     */
    Optional<ExternalGameInstanceEntity> findByLobbyIdAndExternalGameType(UUID lobbyId, String externalGameType);
    
    /**
     * Deletes external game instances for a lobby.
     */
    void deleteByLobbyId(UUID lobbyId);
}





