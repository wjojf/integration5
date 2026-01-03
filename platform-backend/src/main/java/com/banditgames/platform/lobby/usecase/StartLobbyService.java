package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.events.LobbyStartedEvent;
import com.banditgames.platform.lobby.port.in.StartLobbyUseCase;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StartLobbyService implements StartLobbyUseCase {
    
    private final LoadLobbyPort loadLobbyPort;
    private final SaveLobbyPort saveLobbyPort;
    private final PlatformEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public Lobby startLobby(UUID lobbyId, UUID hostId, UUID gameId) {
        Lobby lobby = loadLobbyPort.findById(lobbyId)
            .orElseThrow(() -> new com.banditgames.platform.lobby.domain.exception.LobbyNotFoundException("Lobby not found: " + lobbyId));
        
        if (!lobby.getHostId().equals(hostId)) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException("Only the host can start the lobby");
        }
        
        if (gameId == null) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException("Game ID is required to start the lobby");
        }
        
        // Set the game ID before starting
        lobby.setGameId(gameId);
        
        try {
            lobby.start();
        } catch (IllegalStateException e) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException(e.getMessage(), e);
        }
        
        Lobby saved = saveLobbyPort.save(lobby);
        
        // Publish event
        eventPublisher.publish(new LobbyStartedEvent(
            saved.getId(),
            saved.getGameId(),
            saved.getPlayerIds(),
            saved.getStartedAt()
        ));
        
        return saved;
    }
}

