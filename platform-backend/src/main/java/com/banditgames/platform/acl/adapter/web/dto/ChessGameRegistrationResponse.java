package com.banditgames.platform.acl.adapter.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for chess game registration.
 */
@Data
@AllArgsConstructor
public class ChessGameRegistrationResponse {
    
    @JsonProperty("registrationId")
    private UUID registrationId;
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("gameId")
    private UUID gameId;
}
