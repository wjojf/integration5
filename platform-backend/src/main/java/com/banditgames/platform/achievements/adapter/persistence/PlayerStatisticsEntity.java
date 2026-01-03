package com.banditgames.platform.achievements.adapter.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entity for storing player statistics for achievement evaluation.
 */
@Entity
@Table(name = "player_statistics", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"player_id", "game_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatisticsEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String playerId;
    
    @Column(nullable = false)
    private UUID gameId;
    
    // Win/Loss statistics
    @Column(nullable = false)
    @Builder.Default
    private Integer totalWins = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalLosses = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalDraws = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer currentWinStreak = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer longestWinStreak = 0;
    
    // Time-based statistics (stored as seconds)
    @Column(name = "total_play_time_seconds")
    private Long totalPlayTimeSeconds;
    
    @Column(name = "fastest_win_seconds")
    private Long fastestWinSeconds;
    
    // Social statistics (stored as comma-separated UUIDs)
    @Column(columnDefinition = "TEXT")
    private String uniqueOpponents; // Stored as comma-separated UUIDs
    
    // Game-specific statistics
    @Column(nullable = false)
    @Builder.Default
    private Integer totalMoves = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer totalGames = 0;
}

