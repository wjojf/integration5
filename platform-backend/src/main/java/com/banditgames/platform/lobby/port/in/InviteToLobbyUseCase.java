package com.banditgames.platform.lobby.port.in;

import com.banditgames.platform.lobby.domain.Lobby;

import java.util.UUID;

public interface InviteToLobbyUseCase {
    Lobby inviteToLobby(UUID lobbyId, UUID hostId, UUID invitedPlayerId);
}

