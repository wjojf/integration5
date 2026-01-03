package com.banditgames.platform.lobby.port.in;

import com.banditgames.platform.lobby.domain.Lobby;

import java.util.Optional;
import java.util.UUID;

public interface GetLobbyUseCase {
    Optional<Lobby> getLobby(UUID lobbyId);
    Optional<Lobby> getLobbyByUserId(UUID userId);
}

