package com.banditgames.platform.lobby.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new lobby")
public class CreateLobbyRequest {
    
    @NotNull(message = "Lobby name is required")
    @Size(min = 1, max = 100, message = "Lobby name must be between 1 and 100 characters")
    @Schema(description = "Name of the lobby", example = "My Awesome Lobby", required = true)
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Optional description of the lobby", example = "A fun lobby for casual players")
    private String description;
    
    @NotNull(message = "Max players is required")
    @Min(value = 2, message = "Minimum 2 players required")
    @Max(value = 10, message = "Maximum 10 players allowed")
    @Schema(description = "Maximum number of players in the lobby", example = "4", minimum = "2", maximum = "10", required = true)
    private Integer maxPlayers;
    
    @Schema(description = "Whether the lobby is private (requires invitation) or public", example = "false", defaultValue = "false")
    private boolean isPrivate = false;
}

