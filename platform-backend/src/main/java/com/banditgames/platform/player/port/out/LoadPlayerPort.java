package com.banditgames.platform.player.port.out;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.Rank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPlayerPort {
    Optional<Player> findById(UUID playerId);
    List<Player> findPlayerFriends(UUID playerId);
    Page<Player> searchPlayers(String username, Rank rank, Pageable pageable);
    Page<Player> searchPlayersForUser(UUID userId, String username, Rank rank, Pageable pageable);
}

