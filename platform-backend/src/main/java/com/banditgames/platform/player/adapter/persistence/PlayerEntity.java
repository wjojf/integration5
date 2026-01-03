package com.banditgames.platform.player.adapter.persistence;

import com.banditgames.platform.player.domain.Rank;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "players")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerEntity {

    @Id
    private UUID playerId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(length = 500)
    private String bio;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "player_game_preferences", joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "game_id")
    @Builder.Default
    private List<UUID> gamePreferences = new ArrayList<>();

    @Column(nullable = false)
    private String email;

    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Rank rank = Rank.BRONZE;

    @Column(nullable = false)
    @Builder.Default
    private Integer exp = 0;
}

