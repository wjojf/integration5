package com.banditgames.platform.lobby.domain.events;

import java.util.UUID;

public record LobbyInviteEvent(
    UUID lobbyId,
    UUID gameId,
    UUID hostId,
    UUID invitedPlayerId
) {
}

