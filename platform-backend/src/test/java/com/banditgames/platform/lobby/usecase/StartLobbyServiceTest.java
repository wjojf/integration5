package com.banditgames.platform.lobby.usecase;



import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import com.banditgames.platform.lobby.domain.events.LobbyStartedEvent;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StartLobbyServiceTest {

    private LoadLobbyPort loadLobbyPort;
    private SaveLobbyPort saveLobbyPort;
    private PlatformEventPublisher publisher;
    private StartLobbyService service;

    @BeforeEach
    void setUp() {
        loadLobbyPort = mock(LoadLobbyPort.class);
        saveLobbyPort = mock(SaveLobbyPort.class);
        publisher = mock(PlatformEventPublisher.class);
        service = new StartLobbyService(loadLobbyPort, saveLobbyPort, publisher);
    }

    @Test
    void startLobby_onlyHostCanStart() {
        UUID lobbyId = UUID.randomUUID();
        UUID host = UUID.randomUUID();

        Lobby lobby = Lobby.builder()
                .id(lobbyId)
                .hostId(host)
                .playerIds(new ArrayList<>())
                .status(LobbyStatus.WAITING)
                .maxPlayers(4)
                .visibility(LobbyVisibility.PUBLIC)
                .invitedPlayerIds(new ArrayList<>())
                .build();
        lobby.join(host);

        when(loadLobbyPort.findById(lobbyId)).thenReturn(Optional.of(lobby));

        assertThrows(
                com.banditgames.platform.lobby.domain.exception.LobbyOperationException.class,
                () -> service.startLobby(lobbyId, UUID.randomUUID(), UUID.randomUUID())
        );

        verify(saveLobbyPort, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void startLobby_gameIdRequired() {
        UUID lobbyId = UUID.randomUUID();
        UUID host = UUID.randomUUID();

        Lobby lobby = Lobby.builder()
                .id(lobbyId)
                .hostId(host)
                .playerIds(new ArrayList<>())
                .status(LobbyStatus.WAITING)
                .maxPlayers(4)
                .visibility(LobbyVisibility.PUBLIC)
                .invitedPlayerIds(new ArrayList<>())
                .build();
        lobby.join(host);

        when(loadLobbyPort.findById(lobbyId)).thenReturn(Optional.of(lobby));

        assertThrows(
                com.banditgames.platform.lobby.domain.exception.LobbyOperationException.class,
                () -> service.startLobby(lobbyId, host, null)
        );
    }

    @Test
    void startLobby_setsGameId_starts_saves_andPublishesEvent() {
        UUID lobbyId = UUID.randomUUID();
        UUID host = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();

        Lobby lobby = Lobby.builder()
                .id(lobbyId)
                .hostId(host)
                .playerIds(new ArrayList<>())
                .status(LobbyStatus.WAITING)
                .maxPlayers(4)
                .visibility(LobbyVisibility.PUBLIC)
                .invitedPlayerIds(new ArrayList<>())
                .build();
        lobby.join(host);

        when(loadLobbyPort.findById(lobbyId)).thenReturn(Optional.of(lobby));
        when(saveLobbyPort.save(any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));

        Lobby saved = service.startLobby(lobbyId, host, gameId);

        assertEquals(gameId, saved.getGameId());
        assertEquals(LobbyStatus.STARTED, saved.getStatus());
        assertNotNull(saved.getStartedAt());

        ArgumentCaptor<LobbyStartedEvent> captor = ArgumentCaptor.forClass(LobbyStartedEvent.class);
        verify(publisher).publish(captor.capture());
        assertEquals(lobbyId, captor.getValue().lobbyId());
        assertEquals(gameId, captor.getValue().gameId());
        assertNotNull(captor.getValue().startedAt());
        assertTrue(captor.getValue().playerIds().contains(host));
    }
}

