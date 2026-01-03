package com.banditgames.platform.acl.adapter.messaging.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * Chess game updated message from external chess game service.
 * Routing key: game.player.names.updated
 */
@Data
public class ChessGameUpdatedMessage {
    @JsonProperty("gameId")
    private UUID gameId;
    
    @JsonProperty("whitePlayer")
    private String whitePlayer;
    
    @JsonProperty("blackPlayer")
    private String blackPlayer;
    
    @JsonProperty("currentFen")
    private String currentFen;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("updateType")
    private String updateType; // PLAYERS
    
    @JsonProperty("messageType")
    private String messageType; // GAME_UPDATED
    
    @JsonProperty("timestamp")
    private Date timestamp;
}
