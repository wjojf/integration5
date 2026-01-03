package com.banditgames.platform.acl.adapter.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Request DTO for chess game registration.
 */
@Data
public class ChessGameRegistrationRequest {
    
    @JsonProperty("frontendUrl")
    private String frontendUrl;
    
    @JsonProperty("pictureUrl")
    private String pictureUrl;
}
