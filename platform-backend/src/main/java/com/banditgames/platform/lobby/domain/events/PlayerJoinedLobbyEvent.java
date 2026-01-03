package com.banditgames.platform.lobby.domain.events;

import java.util.UUID;

public record PlayerJoinedLobbyEvent(
    UUID lobbyId,
    UUID playerId
) {
}

