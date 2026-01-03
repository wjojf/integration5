package com.banditgames.platform.player.usecase;

import com.banditgames.platform.player.domain.exception.PlayerNotFoundException;
import com.banditgames.platform.player.port.in.DeletePlayerUseCase;
import com.banditgames.platform.player.port.out.DeletePlayerPort;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeletePlayerService implements DeletePlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final DeletePlayerPort deletePlayerPort;

    @Override
    @Transactional
    public void deletePlayer(UUID playerId) {
        loadPlayerPort.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));
        
        deletePlayerPort.deleteById(playerId);
    }
}

