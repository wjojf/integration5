package com.banditgames.platform.player.port.in;

import java.util.UUID;

public interface DeletePlayerUseCase {
    void deletePlayer(UUID playerId);
}

