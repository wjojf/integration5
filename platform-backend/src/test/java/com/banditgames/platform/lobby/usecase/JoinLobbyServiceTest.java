package com.banditgames.platform.lobby.usecase;



import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import com.banditgames.platform.lobby.domain.events.PlayerJoinedLobbyEvent;
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

class JoinLobbyServiceTest {

    private LoadLobbyPort loadLobbyPort;
    private SaveLobbyPort saveLobbyPort;
    private PlatformEventPublisher publisher;
    private JoinLobbyService service;

    @BeforeEach
    void setUp() {
        loadLobbyPort = mock(LoadLobbyPort.class);
        saveLobbyPort = mock(SaveLobbyPort.class);
        publisher = mock(PlatformEventPublisher.class);
        service = new JoinLobbyService(loadLobbyPort, saveLobbyPort, publisher);
    }

    @Test
    void joinLobby_saves_andPublishesEvent() {
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

        when(loadLobbyPort.findById(lobbyId)).thenReturn(Optional.of(lobby));
        when(saveLobbyPort.save(any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));

        Lobby saved = service.joinLobby(lobbyId, p2);

        assertTrue(saved.getPlayerIds().contains(p2));

        ArgumentCaptor<PlayerJoinedLobbyEvent> captor = ArgumentCaptor.forClass(PlayerJoinedLobbyEvent.class);
        verify(publisher).publish(captor.capture());
        assertEquals(lobbyId, captor.getValue().lobbyId());
        assertEquals(p2, captor.getValue().playerId());
    }
}
