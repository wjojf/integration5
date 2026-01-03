package com.banditgames.platform.player.usecase;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.Rank;
import com.banditgames.platform.player.port.in.SearchPlayersUseCase;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchPlayersService implements SearchPlayersUseCase {

    private final LoadPlayerPort loadPlayerPort;

    @Override
    public Page<Player> searchPlayers(String username, Rank rank, Pageable pageable) {
        return loadPlayerPort.searchPlayers(username, rank, pageable);
    }

    @Override
    public Page<Player> searchPlayersForUser(UUID userId, String username, Rank rank, Pageable pageable) {
        return loadPlayerPort.searchPlayersForUser(userId, username, rank, pageable);
    }

}

