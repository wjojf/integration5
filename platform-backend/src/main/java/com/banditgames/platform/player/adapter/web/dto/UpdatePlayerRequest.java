package com.banditgames.platform.player.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update player profile information")
public class UpdatePlayerRequest {

    @Schema(description = "Updated username (optional)", example = "NewGamerName")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "Updated bio or description (optional)", example = "Competitive player looking for team!")
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    @Schema(description = "Updated address (optional)", example = "456 Oak Ave, Los Angeles, CA")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Schema(description = "Updated list of preferred game IDs (optional)", 
            example = "[\"660e8400-e29b-41d4-a716-446655440001\", \"770e8400-e29b-41d4-a716-446655440002\"]")
    private List<UUID> gamePreferences;
}

