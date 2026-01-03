package com.banditgames.platform.lobby.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to start a lobby with a selected game")
public class StartLobbyRequest {
    
    @NotNull(message = "Game ID is required")
    @Schema(description = "UUID of the game to start", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID gameId;
}

