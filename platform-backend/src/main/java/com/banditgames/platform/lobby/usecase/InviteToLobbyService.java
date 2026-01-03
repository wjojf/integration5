package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.events.LobbyInviteEvent;
import com.banditgames.platform.lobby.port.in.InviteToLobbyUseCase;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteToLobbyService implements InviteToLobbyUseCase {
    
    private final LoadLobbyPort loadLobbyPort;
    private final SaveLobbyPort saveLobbyPort;
    private final PlatformEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public Lobby inviteToLobby(UUID lobbyId, UUID hostId, UUID invitedPlayerId) {
        Lobby lobby = loadLobbyPort.findById(lobbyId)
            .orElseThrow(() -> new com.banditgames.platform.lobby.domain.exception.LobbyNotFoundException("Lobby not found: " + lobbyId));
        
        if (!lobby.getHostId().equals(hostId)) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException("Only the host can invite players");
        }
        
        try {
            lobby.invite(invitedPlayerId);
        } catch (IllegalStateException e) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException(e.getMessage(), e);
        }
        
        Lobby saved = saveLobbyPort.save(lobby);
        
        // Publish event
        eventPublisher.publish(new LobbyInviteEvent(
            saved.getId(),
            saved.getGameId(),
            saved.getHostId(),
            invitedPlayerId
        ));
        
        return saved;
    }
}

