package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.port.in.UpdateLobbyUseCase;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateLobbyService implements UpdateLobbyUseCase {
    
    private final LoadLobbyPort loadLobbyPort;
    private final SaveLobbyPort saveLobbyPort;
    
    @Override
    @Transactional
    public Lobby updateLobby(UUID lobbyId, UUID hostId, String name, String description) {
        Lobby lobby = loadLobbyPort.findById(lobbyId)
            .orElseThrow(() -> new com.banditgames.platform.lobby.domain.exception.LobbyNotFoundException("Lobby not found: " + lobbyId));
        
        if (!lobby.getHostId().equals(hostId)) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException("Only the host can update the lobby");
        }
        
        // Can only update if lobby is in WAITING status
        if (lobby.getStatus() != com.banditgames.platform.lobby.domain.LobbyStatus.WAITING) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException("Can only update lobby when it is in WAITING status");
        }
        
        lobby.setName(name);
        lobby.setDescription(description);
        
        return saveLobbyPort.save(lobby);
    }
}

