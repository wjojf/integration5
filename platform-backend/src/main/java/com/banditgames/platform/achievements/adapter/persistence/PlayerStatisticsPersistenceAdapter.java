package com.banditgames.platform.achievements.adapter.persistence;

import com.banditgames.platform.achievements.domain.PlayerStatistics;
import com.banditgames.platform.achievements.port.out.LoadPlayerStatisticsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter for loading and saving player statistics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerStatisticsPersistenceAdapter implements LoadPlayerStatisticsPort {
    
    private final PlayerStatisticsRepository repository;
    private final PlayerStatisticsMapper mapper;
    
    @Override
    public PlayerStatistics loadStatistics(UUID playerId, UUID gameId) {
        return repository.findByPlayerIdAndGameId(playerId.toString(), gameId)
                .map(mapper::toDomain)
                .orElseGet(() -> {
                    // Initialize new statistics if not found
                    log.debug("Initializing new statistics for player: {}, game: {}", playerId, gameId);
                    return PlayerStatistics.builder()
                            .playerId(playerId)
                            .gameId(gameId)
                            .totalWins(0)
                            .totalLosses(0)
                            .totalDraws(0)
                            .currentWinStreak(0)
                            .longestWinStreak(0)
                            .totalMoves(0)
                            .totalGames(0)
                            .build();
                });
    }
    
    @Override
    public void updateStatistics(UUID playerId, UUID gameId, PlayerStatistics statistics) {
        try {
            PlayerStatisticsEntity entity = mapper.toEntity(statistics);
            
            // Check if entity already exists
            repository.findByPlayerIdAndGameId(playerId.toString(), gameId)
                    .ifPresent(existing -> entity.setId(existing.getId()));
            
            repository.save(entity);
            
            log.debug("Updated statistics for player: {}, game: {}", playerId, gameId);
        } catch (Exception e) {
            log.error("Error updating statistics for player: {}, game: {}", playerId, gameId, e);
            throw new RuntimeException("Failed to update player statistics", e);
        }
    }
}

