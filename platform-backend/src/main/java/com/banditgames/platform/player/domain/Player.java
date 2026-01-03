package com.banditgames.platform.player.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private UUID playerId;
    private String username;
    private String bio; // Extra description text they can modify on their page

    @Builder.Default
    private List<UUID> gamePreferences = new ArrayList<UUID>(); // List of gameIds of games they prefer

    private String email;
    private String address;
    private Rank rank;
    private Integer exp; // XP points gained after each match for rank potentially
}
