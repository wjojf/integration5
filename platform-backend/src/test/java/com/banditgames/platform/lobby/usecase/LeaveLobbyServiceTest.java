package com.banditgames.platform.lobby.usecase;



import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import com.banditgames.platform.lobby.domain.events.PlayerLeftLobbyEvent;
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

class LeaveLobbyServiceTest {

    private LoadLobbyPort loadLobbyPort;
    private SaveLobbyPort saveLobbyPort;
    private PlatformEventPublisher publisher;
    private LeaveLobbyService service;

    @BeforeEach
    void setUp() {
        loadLobbyPort = mock(LoadLobbyPort.class);
        saveLobbyPort = mock(SaveLobbyPort.class);
        publisher = mock(PlatformEventPublisher.class);
        service = new LeaveLobbyService(loadLobbyPort, saveLobbyPort, publisher);
    }

    @Test
    void leaveLobby_saves_andPublishesEvent() {
        UUID lobbyId = UUID.randomUUID();
        UUID host = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

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
        lobby.join(p2);

        when(loadLobbyPort.findById(lobbyId)).thenReturn(Optional.of(lobby));
        when(saveLobbyPort.save(any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));

        Lobby saved = service.leaveLobby(lobbyId, p2);

        assertFalse(saved.getPlayerIds().contains(p2));

        ArgumentCaptor<PlayerLeftLobbyEvent> captor = ArgumentCaptor.forClass(PlayerLeftLobbyEvent.class);
        verify(publisher).publish(captor.capture());
        assertEquals(lobbyId, captor.getValue().lobbyId());
        assertEquals(p2, captor.getValue().playerId());
    }
}

