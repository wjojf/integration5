package com.banditgames.platform.acl.adapter.messaging.chess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * Chess game ended message from external chess game service.
 * Routing key: game.ended
 */
@Data
public class ChessGameEndedMessage {
    @JsonProperty("gameId")
    private UUID gameId;
    
    @JsonProperty("whitePlayer")
    private String whitePlayer;
    
    @JsonProperty("blackPlayer")
    private String blackPlayer;
    
    @JsonProperty("finalFen")
    private String finalFen;
    
    @JsonProperty("endReason")
    private String endReason; // CHECKMATE, DRAW
    
    @JsonProperty("winner")
    private String winner; // WHITE, BLACK, DRAW
    
    @JsonProperty("totalMoves")
    private Integer totalMoves;
    
    @JsonProperty("messageType")
    private String messageType; // GAME_ENDED
    
    @JsonProperty("timestamp")
    private Date timestamp;
}
