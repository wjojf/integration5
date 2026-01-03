package com.banditgames.platform.lobby.port.out;

import com.banditgames.platform.lobby.domain.ExternalGameInstance;

import java.util.Optional;
import java.util.UUID;

public interface LoadExternalGameInstancePort {
    Optional<ExternalGameInstance> findByLobbyId(UUID lobbyId);
    Optional<ExternalGameInstance> findByLobbyIdAndGameType(UUID lobbyId, String gameType);
}



