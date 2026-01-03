package com.banditgames.platform.acl.adapter.dto;

import com.banditgames.platform.player.domain.Rank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for player information exposed through ACL.
 * Contains only public player information (excludes private fields like email and address).
 * This DTO is used to decouple consuming contexts from the Player context's domain model.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInfo {
    private UUID playerId;
    private String username;
    private String bio;
    private List<UUID> gamePreferences;
    private Rank rank;
    private Integer exp;
}

