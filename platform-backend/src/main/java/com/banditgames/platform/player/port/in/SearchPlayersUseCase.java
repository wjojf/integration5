package com.banditgames.platform.player.port.in;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.Rank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SearchPlayersUseCase {
    Page<Player> searchPlayers(String username, Rank rank, Pageable pageable);

    Page<Player> searchPlayersForUser(UUID userId, String username, Rank rank, Pageable pageable);
}

