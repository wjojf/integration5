package com.banditgames.platform.player.usecase;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.port.in.GetPlayerUseCase;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPlayerService implements GetPlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;

    @Override
    public Optional<Player> getPlayer(UUID playerId) {
        return loadPlayerPort.findById(playerId);
    }

    @Override
    public List<Player> getPlayerFriends(UUID playerId) {
        return loadPlayerPort.findPlayerFriends(playerId);
    }
}

