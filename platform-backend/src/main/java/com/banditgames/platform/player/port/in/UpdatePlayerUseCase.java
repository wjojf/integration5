package com.banditgames.platform.player.port.in;

import com.banditgames.platform.player.domain.Player;

import java.util.List;
import java.util.UUID;

public interface UpdatePlayerUseCase {
    Player updatePlayer(UUID playerId, String username, String bio, String address, List<UUID> gamePreferences);
}

