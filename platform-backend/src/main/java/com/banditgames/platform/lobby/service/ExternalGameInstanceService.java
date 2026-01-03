package com.banditgames.platform.lobby.service;

import com.banditgames.platform.lobby.domain.ExternalGameInstance;
import com.banditgames.platform.lobby.port.out.LoadExternalGameInstancePort;
import com.banditgames.platform.lobby.port.out.SaveExternalGameInstancePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service to manage external game instance mappings for lobbies.
 * 
 * Generic service that supports multiple external game types (chess, checkers, etc.)
 * without polluting the lobby table with game-specific fields.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalGameInstanceService {
    
    private final LoadExternalGameInstancePort loadExternalGameInstancePort;
    private final SaveExternalGameInstancePort saveExternalGameInstancePort;
    
    /**
     * Stores an external game instance mapping for a lobby.
     * 
     * @param lobbyId The lobby ID
     * @param gameId The platform game ID
     * @param externalGameType The external game type (e.g., "chess", "checkers")
     * @param externalGameInstanceId The external game instance ID
     */
    @Transactional
    public void storeExternalGameInstance(UUID lobbyId, UUID gameId, String externalGameType, UUID externalGameInstanceId) {
        ExternalGameInstance instance = ExternalGameInstance.builder()
                .lobbyId(lobbyId)
                .gameId(gameId)
                .externalGameType(externalGameType)
                .externalGameInstanceId(externalGameInstanceId)
                .build();
        
        saveExternalGameInstancePort.save(instance);
        
        log.info("Stored external game instance - lobbyId={}, gameId={}, type={}, instanceId={}", 
                lobbyId, gameId, externalGameType, externalGameInstanceId);
    }
    
    /**
     * Retrieves the external game instance ID for a given lobby.
     * 
     * @param lobbyId The lobby ID
     * @return The external game instance ID, or null if not found
     */
    public UUID getExternalGameInstanceId(UUID lobbyId) {
        return loadExternalGameInstancePort.findByLobbyId(lobbyId)
                .map(ExternalGameInstance::getExternalGameInstanceId)
                .orElse(null);
    }
    
    /**
     * Retrieves the external game instance for a given lobby and game type.
     * 
     * @param lobbyId The lobby ID
     * @param gameType The external game type
     * @return The external game instance, or null if not found
     */
    public ExternalGameInstance getExternalGameInstance(UUID lobbyId, String gameType) {
        return loadExternalGameInstancePort.findByLobbyIdAndGameType(lobbyId, gameType)
                .orElse(null);
    }
    
    /**
     * Checks if a lobby has an associated external game instance.
     * 
     * @param lobbyId The lobby ID
     * @return true if the lobby has an external game instance
     */
    public boolean hasExternalGameInstance(UUID lobbyId) {
        return loadExternalGameInstancePort.findByLobbyId(lobbyId).isPresent();
    }
    
    /**
     * Removes external game instance mapping for a lobby.
     * 
     * @param lobbyId The lobby ID
     */
    @Transactional
    public void removeExternalGameInstance(UUID lobbyId) {
        saveExternalGameInstancePort.deleteByLobbyId(lobbyId);
        log.debug("Removed external game instance mapping - lobbyId={}", lobbyId);
    }
}

