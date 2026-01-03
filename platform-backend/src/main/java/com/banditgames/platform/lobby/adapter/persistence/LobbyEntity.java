package com.banditgames.platform.lobby.adapter.persistence;

import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lobbies")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LobbyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = true)
    private UUID gameId;

    @Column(nullable = true)
    private UUID sessionId;

    @Column(nullable = false)
    private UUID hostId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lobby_players", joinColumns = @JoinColumn(name = "lobby_id"))
    @Column(name = "player_id")
    @Builder.Default
    private List<UUID> playerIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LobbyStatus status;

    @Column(nullable = false)
    private Integer maxPlayers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LobbyVisibility visibility;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lobby_invites", joinColumns = @JoinColumn(name = "lobby_id"))
    @Column(name = "player_id")
    @Builder.Default
    private List<UUID> invitedPlayerIds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = LobbyStatus.WAITING;
        }
        if (name == null || name.trim().isEmpty()) {
            name = "Untitled Lobby";
        }
    }
    
    @PostLoad
    protected void onLoad() {
        // Ensure name is never null when loading from database
        // This handles existing lobbies that might have been created before name was required
        if (name == null || name.trim().isEmpty()) {
            name = "Untitled Lobby";
        }
    }
}
