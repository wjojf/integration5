package com.banditgames.platform.lobby.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to update lobby name and description")
public class UpdateLobbyRequest {
    
    @NotNull(message = "Lobby name is required")
    @Size(min = 1, max = 100, message = "Lobby name must be between 1 and 100 characters")
    @Schema(description = "Name of the lobby", example = "My Updated Lobby", required = true)
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Optional description of the lobby", example = "An updated description")
    private String description;
}

