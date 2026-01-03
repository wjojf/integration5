package com.banditgames.platform.lobby.port.out;

import com.banditgames.platform.lobby.domain.Lobby;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadLobbyPort {
    Optional<Lobby> findById(UUID lobbyId);
    Optional<Lobby> findByHostIdAndStatus(UUID hostId, com.banditgames.platform.lobby.domain.LobbyStatus status);
    Optional<Lobby> findBySessionId(UUID sessionId);
    Page<Lobby> searchLobbies(UUID gameId, List<UUID> hostIds, Pageable pageable);
    Optional<Lobby> findPlayerLobby(UUID playerId);
}

