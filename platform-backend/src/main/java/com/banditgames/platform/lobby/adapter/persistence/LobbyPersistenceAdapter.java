package com.banditgames.platform.lobby.adapter.persistence;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LobbyPersistenceAdapter implements LoadLobbyPort, SaveLobbyPort {
    
    private final LobbyRepository lobbyRepository;
    private final LobbyMapper lobbyMapper;
    
    @Override
    public Optional<Lobby> findById(UUID lobbyId) {
        return lobbyRepository.findById(lobbyId)
            .map(lobbyMapper::toDomain);
    }
    
    @Override
    public Optional<Lobby> findByHostIdAndStatus(UUID hostId, LobbyStatus status) {
        return lobbyRepository.findByHostIdAndStatus(hostId, status)
            .map(lobbyMapper::toDomain);
    }
    
    @Override
    public Optional<Lobby> findBySessionId(UUID sessionId) {
        return lobbyRepository.findBySessionId(sessionId)
            .map(lobbyMapper::toDomain);
    }
    
    @Override
    public Page<Lobby> searchLobbies(UUID gameId, List<UUID> hostIds, Pageable pageable) {
        Page<LobbyEntity> entities;
        
        if (gameId != null && hostIds != null && !hostIds.isEmpty()) {
            entities = lobbyRepository.findByGameIdAndHostIdInAndStatusAndVisibility(
                gameId, hostIds, LobbyStatus.WAITING, LobbyVisibility.PUBLIC, pageable
            );
        } else if (gameId != null) {
            entities = lobbyRepository.findByGameIdAndStatusAndVisibility(
                gameId, LobbyStatus.WAITING, LobbyVisibility.PUBLIC, pageable
            );
        } else if (hostIds != null && !hostIds.isEmpty()) {
            entities = lobbyRepository.findByHostIdInAndStatusAndVisibility(
                hostIds, LobbyStatus.WAITING, LobbyVisibility.PUBLIC, pageable
            );
        } else {
            entities = lobbyRepository.findByStatusAndVisibility(
                LobbyStatus.WAITING, LobbyVisibility.PUBLIC, pageable
            );
        }
        
        return entities.map(lobbyMapper::toDomain);
    }
    
    @Override
    public Optional<Lobby> findPlayerLobby(UUID playerId) {
        // A player can only be in ONE lobby at a time
        // Find active lobbies (not CANCELLED or COMPLETED) for this player
        List<LobbyEntity> entities = lobbyRepository.findByPlayerIdsContaining(playerId);
        
        // Filter for active lobbies and return the first one (should only be one)
        return entities.stream()
                .filter(entity -> entity.getStatus() != LobbyStatus.CANCELLED 
                        && entity.getStatus() != LobbyStatus.COMPLETED)
                .findFirst()
                .map(lobbyMapper::toDomain);
    }
    
    @Override
    public Lobby save(Lobby lobby) {
        LobbyEntity entity = lobbyMapper.toEntity(lobby);
        LobbyEntity saved = lobbyRepository.save(entity);
        return lobbyMapper.toDomain(saved);
    }
}
