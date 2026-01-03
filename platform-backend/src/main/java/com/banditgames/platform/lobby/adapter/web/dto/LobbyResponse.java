package com.banditgames.platform.lobby.adapter.web.dto;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class LobbyResponse {
    private UUID id;
    private UUID gameId;
    private UUID sessionId;
    private UUID hostId;
    private String name;
    private String description;
    private List<UUID> playerIds;
    private LobbyStatus status;
    private Integer maxPlayers;
    private LobbyVisibility visibility;
    private List<UUID> invitedPlayerIds;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;

    public static LobbyResponse fromDomain(Lobby lobby) {
        return LobbyResponse.builder()
            .id(lobby.getId())
            .gameId(lobby.getGameId())
            .sessionId(lobby.getSessionId())
            .hostId(lobby.getHostId())
            .name(lobby.getName())
            .description(lobby.getDescription())
            .playerIds(lobby.getPlayerIds())
            .status(lobby.getStatus())
            .maxPlayers(lobby.getMaxPlayers())
            .visibility(lobby.getVisibility())
            .invitedPlayerIds(lobby.getInvitedPlayerIds())
            .createdAt(lobby.getCreatedAt())
            .startedAt(lobby.getStartedAt())
            .build();
    }
}
