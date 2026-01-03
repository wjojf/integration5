package com.banditgames.platform.achievements.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Player statistics for achievement evaluation.
 * 
 * This domain object tracks player performance metrics needed
 * to evaluate achievement criteria.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStatistics {
    
    private UUID playerId;
    private UUID gameId;
    
    // Win/Loss statistics
    private int totalWins;
    private int totalLosses;
    private int totalDraws;
    private int currentWinStreak;
    private int longestWinStreak;
    
    // Time-based statistics
    private Duration totalPlayTime;
    private Duration fastestWin;
    
    // Social statistics
    private Set<UUID> uniqueOpponents;
    
    // Game-specific statistics
    private int totalMoves;
    private int totalGames;
    
    /**
     * Increments win count and updates streak.
     */
    public void recordWin() {
        this.totalWins++;
        this.totalGames++;
        this.currentWinStreak++;
        if (this.currentWinStreak > this.longestWinStreak) {
            this.longestWinStreak = this.currentWinStreak;
        }
    }
    
    /**
     * Increments loss count and resets streak.
     */
    public void recordLoss() {
        this.totalLosses++;
        this.totalGames++;
        this.currentWinStreak = 0;
    }
    
    /**
     * Increments draw count.
     */
    public void recordDraw() {
        this.totalDraws++;
        this.totalGames++;
        this.currentWinStreak = 0;
    }
    
    /**
     * Adds play time.
     */
    public void addPlayTime(Duration duration) {
        if (this.totalPlayTime == null) {
            this.totalPlayTime = Duration.ZERO;
        }
        this.totalPlayTime = this.totalPlayTime.plus(duration);
    }
    
    /**
     * Records a game against an opponent.
     */
    public void recordOpponent(UUID opponentId) {
        if (this.uniqueOpponents == null) {
            this.uniqueOpponents = new java.util.HashSet<>();
        }
        this.uniqueOpponents.add(opponentId);
    }
    
    /**
     * Records a move.
     */
    public void recordMove() {
        this.totalMoves++;
    }
    
    /**
     * Updates fastest win time if this is faster.
     */
    public void updateFastestWin(Duration duration) {
        if (this.fastestWin == null || duration.compareTo(this.fastestWin) < 0) {
            this.fastestWin = duration;
        }
    }
}

