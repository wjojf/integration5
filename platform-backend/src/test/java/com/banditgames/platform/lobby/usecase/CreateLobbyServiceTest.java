package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import com.banditgames.platform.lobby.domain.events.LobbyCreatedEvent;
import com.banditgames.platform.lobby.port.out.SaveLobbyPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateLobbyServiceTest {

    private SaveLobbyPort saveLobbyPort;
    private PlatformEventPublisher publisher;
    private CreateLobbyService service;

    @BeforeEach
    void setUp() {
        saveLobbyPort = mock(SaveLobbyPort.class);
        publisher = mock(PlatformEventPublisher.class);
        service = new CreateLobbyService(saveLobbyPort, publisher);
    }

    @Test
    void createLobby_hostAutoJoins_saves_andPublishesEvent() {
        UUID host = UUID.randomUUID();

        when(saveLobbyPort.save(any(Lobby.class))).thenAnswer(inv -> {
            Lobby l = inv.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        Lobby lobby = service.createLobby(host, 4, false);

        assertEquals(host, lobby.getHostId());
        assertEquals(LobbyStatus.WAITING, lobby.getStatus());
        assertEquals(4, lobby.getMaxPlayers());
        assertEquals(LobbyVisibility.PUBLIC, lobby.getVisibility());
        assertNotNull(lobby.getId());
        assertNotNull(lobby.getCreatedAt());
        assertTrue(lobby.getPlayerIds().contains(host)); // host auto joins

        ArgumentCaptor<LobbyCreatedEvent> captor = ArgumentCaptor.forClass(LobbyCreatedEvent.class);
        verify(publisher).publish(captor.capture());
        assertEquals(lobby.getId(), captor.getValue().lobbyId());
        assertEquals(host, captor.getValue().hostId());
    }
}
