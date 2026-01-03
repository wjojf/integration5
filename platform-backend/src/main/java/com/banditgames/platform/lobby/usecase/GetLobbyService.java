package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.port.in.GetLobbyUseCase;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetLobbyService implements GetLobbyUseCase {
    
    private final LoadLobbyPort loadLobbyPort;
    
    @Override
    public Optional<Lobby> getLobby(UUID lobbyId) {
        return loadLobbyPort.findById(lobbyId);
    }
    
    @Override
    public Optional<Lobby> getLobbyByUserId(UUID userId) {
        return loadLobbyPort.findPlayerLobby(userId);
    }
}

