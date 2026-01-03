package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.events.PlayerLeftLobbyEvent;
import com.banditgames.platform.lobby.port.in.LeaveLobbyUseCase;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveLobbyService implements LeaveLobbyUseCase {
    
    private final LoadLobbyPort loadLobbyPort;
    private final SaveLobbyPort saveLobbyPort;
    private final PlatformEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public Lobby leaveLobby(UUID lobbyId, UUID playerId) {
        Lobby lobby = loadLobbyPort.findById(lobbyId)
            .orElseThrow(() -> new com.banditgames.platform.lobby.domain.exception.LobbyNotFoundException("Lobby not found: " + lobbyId));
        
        try {
            lobby.leave(playerId);
        } catch (IllegalStateException e) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException(e.getMessage(), e);
        }
        
        Lobby saved = saveLobbyPort.save(lobby);
        
        // Publish event
        eventPublisher.publish(new PlayerLeftLobbyEvent(
            saved.getId(),
            playerId
        ));
        
        return saved;
    }
}

