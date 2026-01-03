package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import com.banditgames.platform.lobby.domain.events.LobbyCreatedEvent;
import com.banditgames.platform.lobby.port.in.CreateLobbyUseCase;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateLobbyService implements CreateLobbyUseCase {

    private final LoadLobbyPort loadLobbyPort;
    private final SaveLobbyPort saveLobbyPort;
    private final PlatformEventPublisher eventPublisher;

    @Override
    @Transactional
    public Lobby createLobby(UUID hostId, String name, String description, Integer maxPlayers, boolean isPrivate) {
        // A player can only be in ONE lobby at a time
        // Check if player is already in an active lobby
        loadLobbyPort.findPlayerLobby(hostId).ifPresent(existingLobby -> {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException(
                "Player is already in an active lobby. A player can only be in one lobby at a time. Please leave your current lobby before creating a new one."
            );
        });
        
        Lobby lobby = Lobby.builder()
            .hostId(hostId)
            .name(name)
            .description(description)
            .playerIds(new ArrayList<>())
            .status(LobbyStatus.WAITING)
            .maxPlayers(maxPlayers)
            .visibility(isPrivate ? LobbyVisibility.PRIVATE : LobbyVisibility.PUBLIC)
            .invitedPlayerIds(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .build();

        // Host automatically joins
        try {
            lobby.join(hostId);
        } catch (IllegalStateException e) {
            throw new com.banditgames.platform.lobby.domain.exception.LobbyOperationException(e.getMessage(), e);
        }

        Lobby saved = saveLobbyPort.save(lobby);

        // Publish event
        eventPublisher.publish(new LobbyCreatedEvent(
            saved.getId(),
            saved.getGameId(),
            saved.getHostId(),
            saved.getMaxPlayers(),
            saved.getCreatedAt()
        ));

        return saved;
    }
}

