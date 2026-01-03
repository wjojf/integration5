package com.banditgames.platform.player.usecase;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.exception.PlayerNotFoundException;
import com.banditgames.platform.player.port.in.UpdatePlayerUseCase;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import com.banditgames.platform.player.port.out.SavePlayerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdatePlayerService implements UpdatePlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final SavePlayerPort savePlayerPort;

    @Override
    @Transactional
    public Player updatePlayer(UUID playerId, String username, String bio, String address, List<UUID> gamePreferences) {
        Player player = loadPlayerPort.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));

        if (username != null) {
            player.setUsername(username);
        }
        if (bio != null) {
            player.setBio(bio);
        }
        if (address != null) {
            player.setAddress(address);
        }
        if (gamePreferences != null) {
            player.setGamePreferences(gamePreferences);
        }

        return savePlayerPort.save(player);
    }
}

