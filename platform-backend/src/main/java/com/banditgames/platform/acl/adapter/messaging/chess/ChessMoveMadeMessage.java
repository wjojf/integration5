package com.banditgames.platform.acl.adapter.messaging.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * Chess move made message from external chess game service.
 * Routing key: move.made
 */
@Data
public class ChessMoveMadeMessage {
    @JsonProperty("gameId")
    private UUID gameId;
    
    @JsonProperty("fromSquare")
    private String fromSquare; // e2
    
    @JsonProperty("toSquare")
    private String toSquare; // e4
    
    @JsonProperty("sanNotation")
    private String sanNotation; // e4
    
    @JsonProperty("fenAfterMove")
    private String fenAfterMove;
    
    @JsonProperty("player")
    private String player; // WHITE, BLACK
    
    @JsonProperty("moveNumber")
    private Integer moveNumber;
    
    @JsonProperty("whitePlayer")
    private String whitePlayer;
    
    @JsonProperty("blackPlayer")
    private String blackPlayer;
    
    @JsonProperty("moveTime")
    private Date moveTime;
    
    @JsonProperty("messageType")
    private String messageType; // MOVE_MADE
    
    @JsonProperty("timestamp")
    private Date timestamp;
}
