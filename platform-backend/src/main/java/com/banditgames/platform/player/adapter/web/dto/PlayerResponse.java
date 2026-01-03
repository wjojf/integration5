package com.banditgames.platform.player.adapter.web.dto;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.Rank;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Player profile information")
public class PlayerResponse {
    
    @Schema(description = "Unique identifier of the player", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID playerId;
    
    @Schema(description = "Player's username", example = "GamerPro2024")
    private String username;
    
    @Schema(description = "Player's bio or description", example = "Competitive gamer, love strategy games!")
    private String bio;
    
    @Schema(description = "List of preferred game IDs", example = "[\"660e8400-e29b-41d4-a716-446655440001\", \"770e8400-e29b-41d4-a716-446655440002\"]")
    private List<UUID> gamePreferences;
    
    @Schema(description = "Player's email address", example = "gamerpro@example.com")
    private String email;
    
    @Schema(description = "Player's address", example = "123 Main St, New York, NY")
    private String address;
    
    @Schema(description = "Player's rank", example = "GOLD")
    private Rank rank;
    
    @Schema(description = "Player's experience points", example = "2500")
    private Integer exp;

    public static PlayerResponse fromDomain(Player player) {
        return PlayerResponse.builder()
                .playerId(player.getPlayerId())
                .username(player.getUsername())
                .bio(player.getBio())
                .gamePreferences(player.getGamePreferences())
                .email(player.getEmail())
                .address(player.getAddress())
                .rank(player.getRank())
                .exp(player.getExp())
                .build();
    }

    public static PlayerResponse fromDomainWithoutPrivateInfo(Player player) {
        return PlayerResponse.builder()
                .playerId(player.getPlayerId())
                .username(player.getUsername())
                .bio(player.getBio())
                .gamePreferences(player.getGamePreferences())
                .email(null)
                .address(null)
                .rank(player.getRank())
                .exp(player.getExp())
                .build();
    }
}

