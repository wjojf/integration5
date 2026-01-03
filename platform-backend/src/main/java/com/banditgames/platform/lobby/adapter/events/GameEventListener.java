package com.banditgames.platform.lobby.adapter.events;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import com.banditgames.platform.shared.events.GameEndedDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventListener {

    private final LoadLobbyPort loadLobbyPort;
    private final SaveLobbyPort saveLobbyPort;

    @ApplicationModuleListener
    public void onGameEnded(GameEndedDomainEvent event) {
        log.info("Received GameEndedDomainEvent for lobby: {}", event.lobbyId());

        try {
            Lobby lobby = loadLobbyPort.findById(event.lobbyId())
                .orElseThrow(() -> new IllegalArgumentException("Lobby not found: " + event.lobbyId()));

            // Reset the lobby after game ends: clear sessionId and reset status to WAITING
            // This allows players to start a new game in the same lobby
            lobby.resetAfterGameEnd();
            
            saveLobbyPort.save(lobby);

            log.info("Lobby {} session cleared and reset to WAITING after game ended", event.lobbyId());
        } catch (Exception e) {
            log.error("Failed to update lobby after game ended: {}", event.lobbyId(), e);
            // Don't throw - this is an async event handler and we don't want to break the game ending
        }
    }
}

