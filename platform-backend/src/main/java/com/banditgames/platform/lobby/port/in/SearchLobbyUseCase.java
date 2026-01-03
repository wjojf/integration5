package com.banditgames.platform.lobby.port.in;

import com.banditgames.platform.lobby.domain.Lobby;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SearchLobbyUseCase {
    Page<Lobby> searchLobbies(UUID gameId, List<UUID> hostIds, Pageable pageable);
}
