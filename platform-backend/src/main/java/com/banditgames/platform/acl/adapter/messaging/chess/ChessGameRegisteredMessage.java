package com.banditgames.platform.acl.adapter.messaging.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Chess game registered message from external chess game service.
 * Routing key: game.registered
 */
@Data
public class ChessGameRegisteredMessage {
    @JsonProperty("registrationId")
    private UUID registrationId;
    
    @JsonProperty("frontendUrl")
    private String frontendUrl;
    
    @JsonProperty("pictureUrl")
    private String pictureUrl;
    
    @JsonProperty("availableAchievements")
    private List<ChessAchievement> availableAchievements;
    
    @JsonProperty("messageType")
    private String messageType; // GAME_REGISTERED
    
    @JsonProperty("timestamp")
    private Date timestamp;
    
    @Data
    public static class ChessAchievement {
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("description")
        private String description;
    }
}
