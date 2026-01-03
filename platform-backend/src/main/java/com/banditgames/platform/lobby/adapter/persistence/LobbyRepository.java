package com.banditgames.platform.lobby.adapter.persistence;

import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LobbyRepository extends JpaRepository<LobbyEntity, UUID> {
    
    List<LobbyEntity> findByStatus(LobbyStatus status);
    
    List<LobbyEntity> findByGameId(UUID gameId);
    
    List<LobbyEntity> findByVisibility(LobbyVisibility visibility);
    
    List<LobbyEntity> findByStatusAndVisibility(LobbyStatus status, LobbyVisibility visibility);
    
    List<LobbyEntity> findByPlayerIdsContaining(UUID playerId);
    
    List<LobbyEntity> findByPlayerIdsContainingAndStatusIn(UUID playerId, List<LobbyStatus> statuses);
    
    List<LobbyEntity> findByInvitedPlayerIdsContaining(UUID playerId);
    
    Optional<LobbyEntity> findByHostIdAndStatus(UUID hostId, LobbyStatus status);
    
    Optional<LobbyEntity> findBySessionId(UUID sessionId);
    
    List<LobbyEntity> findByGameIdAndStatusAndVisibility(UUID gameId, LobbyStatus status, LobbyVisibility visibility);
    
    List<LobbyEntity> findByHostIdInAndStatusAndVisibility(List<UUID> hostIds, LobbyStatus status, LobbyVisibility visibility);
    
    List<LobbyEntity> findByGameIdAndHostIdInAndStatusAndVisibility(UUID gameId, List<UUID> hostIds, LobbyStatus status, LobbyVisibility visibility);
    
    // Pageable versions for search
    Page<LobbyEntity> findByStatusAndVisibility(LobbyStatus status, LobbyVisibility visibility, Pageable pageable);
    
    Page<LobbyEntity> findByGameIdAndStatusAndVisibility(UUID gameId, LobbyStatus status, LobbyVisibility visibility, Pageable pageable);
    
    Page<LobbyEntity> findByHostIdInAndStatusAndVisibility(List<UUID> hostIds, LobbyStatus status, LobbyVisibility visibility, Pageable pageable);
    
    Page<LobbyEntity> findByGameIdAndHostIdInAndStatusAndVisibility(UUID gameId, List<UUID> hostIds, LobbyStatus status, LobbyVisibility visibility, Pageable pageable);
}
