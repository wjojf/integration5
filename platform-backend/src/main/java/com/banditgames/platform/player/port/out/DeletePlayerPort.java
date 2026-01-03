package com.banditgames.platform.player.port.out;

import java.util.UUID;

public interface DeletePlayerPort {
    void deleteById(UUID playerId);
}

