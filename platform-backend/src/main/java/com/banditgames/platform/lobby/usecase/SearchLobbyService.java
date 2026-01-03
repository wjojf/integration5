package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.port.in.SearchLobbyUseCase;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchLobbyService implements SearchLobbyUseCase {
    
    private final LoadLobbyPort loadLobbyPort;
    
    @Override
    public Page<Lobby> searchLobbies(UUID gameId, List<UUID> hostIds, Pageable pageable) {
        return loadLobbyPort.searchLobbies(gameId, hostIds, pageable);
    }
}
