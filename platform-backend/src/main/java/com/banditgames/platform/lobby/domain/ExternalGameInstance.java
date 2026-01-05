package com.banditgames.platform.lobby.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Domain model for external game instance mapping.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalGameInstance {
    private UUID id;
    private UUID lobbyId;
    private UUID gameId; // Platform game ID
    private String externalGameType; // e.g., "chess", "checkers"
    private UUID externalGameInstanceId; // The actual game instance ID in the external service
}








