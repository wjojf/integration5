package com.banditgames.platform.player.port.out;

import com.banditgames.platform.player.domain.Player;

public interface SavePlayerPort {
    Player save(Player player);
}

