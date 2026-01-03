package com.banditgames.platform.lobby.port.in;

import com.banditgames.platform.lobby.domain.Lobby;

import java.util.UUID;

public interface CreateLobbyUseCase {
    Lobby createLobby(UUID hostId, String name, String description, Integer maxPlayers, boolean isPrivate);
}

