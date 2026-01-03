package com.banditgames.platform.acl.adapter.messaging.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * Chess game created message from external chess game service.
 * Routing key: game.created
 */
@Data
public class ChessGameCreatedMessage {
    @JsonProperty("gameId")
    private UUID gameId;
    
    @JsonProperty("whitePlayer")
    private String whitePlayer;
    
    @JsonProperty("blackPlayer")
    private String blackPlayer;
    
    @JsonProperty("currentFen")
    private String currentFen;
    
    @JsonProperty("status")
    private String status; // ACTIVE, etc.
    
    @JsonProperty("messageType")
    private String messageType; // GAME_CREATED
    
    @JsonProperty("timestamp")
    private Date timestamp;
}
