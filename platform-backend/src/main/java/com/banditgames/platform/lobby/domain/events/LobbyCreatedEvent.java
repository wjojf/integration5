package com.banditgames.platform.lobby.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record LobbyCreatedEvent(
    UUID lobbyId,
    UUID gameId,
    UUID hostId,
    Integer maxPlayers,
    LocalDateTime createdAt
) {
}

