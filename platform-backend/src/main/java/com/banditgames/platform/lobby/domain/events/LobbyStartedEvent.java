package com.banditgames.platform.lobby.domain.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LobbyStartedEvent(
    UUID lobbyId,
    UUID gameId,
    List<UUID> playerIds,
    LocalDateTime startedAt
) {
}

