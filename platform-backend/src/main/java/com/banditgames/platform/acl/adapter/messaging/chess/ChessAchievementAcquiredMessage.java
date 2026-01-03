package com.banditgames.platform.acl.adapter.messaging.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * Chess achievement acquired message from external chess game service.
 * Routing key: achievement.acquired
 */
@Data
public class ChessAchievementAcquiredMessage {
    @JsonProperty("gameId")
    private UUID gameId;
    
    @JsonProperty("playerId")
    private UUID playerId;
    
    @JsonProperty("playerName")
    private String playerName;
    
    @JsonProperty("achievementType")
    private String achievementType; // FIRST_BLOOD, PAWN_POWER, etc.
    
    @JsonProperty("achievementDescription")
    private String achievementDescription;
    
    @JsonProperty("messageType")
    private String messageType; // ACHIEVEMENT_ACQUIRED
    
    @JsonProperty("timestamp")
    private Date timestamp;
}
