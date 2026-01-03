package com.banditgames.platform.player.port.in;

import com.banditgames.platform.player.domain.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetPlayerUseCase {
    Optional<Player> getPlayer(UUID playerId);

    List<Player> getPlayerFriends(UUID playerId);
}

