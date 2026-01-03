package com.banditgames.platform.lobby.port.out;

import com.banditgames.platform.lobby.domain.ExternalGameInstance;

public interface SaveExternalGameInstancePort {
    ExternalGameInstance save(ExternalGameInstance externalGameInstance);
    void deleteByLobbyId(java.util.UUID lobbyId);
}





