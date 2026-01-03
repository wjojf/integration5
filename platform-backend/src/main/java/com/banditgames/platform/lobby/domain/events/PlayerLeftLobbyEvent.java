package com.banditgames.platform.lobby.domain.events;

import java.util.UUID;

public record PlayerLeftLobbyEvent(
    UUID lobbyId,
    UUID playerId
) {
}

