package com.banditgames.platform.achievements.adapter.persistence;

import com.banditgames.platform.achievements.domain.PlayerStatistics;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper between PlayerStatistics domain and entity.
 */
@Component
public class PlayerStatisticsMapper {
    
    public PlayerStatistics toDomain(PlayerStatisticsEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return PlayerStatistics.builder()
                .playerId(UUID.fromString(entity.getPlayerId()))
                .gameId(entity.getGameId())
                .totalWins(entity.getTotalWins())
                .totalLosses(entity.getTotalLosses())
                .totalDraws(entity.getTotalDraws())
                .currentWinStreak(entity.getCurrentWinStreak())
                .longestWinStreak(entity.getLongestWinStreak())
                .totalPlayTime(entity.getTotalPlayTimeSeconds() != null 
                        ? Duration.ofSeconds(entity.getTotalPlayTimeSeconds()) 
                        : null)
                .fastestWin(entity.getFastestWinSeconds() != null 
                        ? Duration.ofSeconds(entity.getFastestWinSeconds()) 
                        : null)
                .uniqueOpponents(parseUniqueOpponents(entity.getUniqueOpponents()))
                .totalMoves(entity.getTotalMoves())
                .totalGames(entity.getTotalGames())
                .build();
    }
    
    public PlayerStatisticsEntity toEntity(PlayerStatistics statistics) {
        if (statistics == null) {
            return null;
        }
        
        return PlayerStatisticsEntity.builder()
                .id(null) // Let JPA generate
                .playerId(statistics.getPlayerId().toString())
                .gameId(statistics.getGameId())
                .totalWins(statistics.getTotalWins())
                .totalLosses(statistics.getTotalLosses())
                .totalDraws(statistics.getTotalDraws())
                .currentWinStreak(statistics.getCurrentWinStreak())
                .longestWinStreak(statistics.getLongestWinStreak())
                .totalPlayTimeSeconds(statistics.getTotalPlayTime() != null 
                        ? statistics.getTotalPlayTime().getSeconds() 
                        : null)
                .fastestWinSeconds(statistics.getFastestWin() != null 
                        ? statistics.getFastestWin().getSeconds() 
                        : null)
                .uniqueOpponents(formatUniqueOpponents(statistics.getUniqueOpponents()))
                .totalMoves(statistics.getTotalMoves())
                .totalGames(statistics.getTotalGames())
                .build();
    }
    
    private Set<UUID> parseUniqueOpponents(String opponentsStr) {
        if (opponentsStr == null || opponentsStr.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        return Arrays.stream(opponentsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }
    
    private String formatUniqueOpponents(Set<UUID> opponents) {
        if (opponents == null || opponents.isEmpty()) {
            return null;
        }
        
        return opponents.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","));
    }
}

