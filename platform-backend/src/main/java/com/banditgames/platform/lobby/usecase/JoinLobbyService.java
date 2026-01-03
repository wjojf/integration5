package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.events.PlayerJoinedLobbyEvent;
import com.banditgames.platform.lobby.port.in.JoinLobbyUseCase;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JoinLobbyService implements JoinLobbyUseCase {
    
    private final LoadLobbyPort loadLobbyPort;
    private final SaveLobbyPort saveLobbyPort;
    private final PlatformEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public Lobby joinLobby(UUID lobbyId, UUID playerId) {
        // A player can only be in ONE lobby at a time
        // Check if player is already in another active lobby
        loadLobbyPort.findPlayerLobby(playerId).ifPresent(existingLobby -> {
            if (!existingLobby.getId().equals(lobbyId)) {
                throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException(
                    "Player is already in an active lobby. A player can only be in one lobby at a time. Please leave your current lobby before joining another one."
                );
            }
        });
        
        Lobby lobby = loadLobbyPort.findById(lobbyId)
            .orElseThrow(() -> new com.banditgames.platform.lobby.domain.exception.LobbyNotFoundException("Lobby not found: " + lobbyId));
        
        try {
            lobby.join(playerId);
        } catch (IllegalStateException e) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException(e.getMessage(), e);
        }
        
        Lobby saved = saveLobbyPort.save(lobby);
        
        // Publish event
        eventPublisher.publish(new PlayerJoinedLobbyEvent(
            saved.getId(),
            playerId
        ));
        
        return saved;
    }
}

