package com.banditgames.platform.lobby.port.in;

import com.banditgames.platform.lobby.domain.Lobby;

import java.util.UUID;

public interface UpdateLobbyUseCase {
    Lobby updateLobby(UUID lobbyId, UUID hostId, String name, String description);
}

