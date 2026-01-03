package com.banditgames.platform.lobby.port.out;

import com.banditgames.platform.lobby.domain.Lobby;

public interface SaveLobbyPort {
    Lobby save(Lobby lobby);
}

