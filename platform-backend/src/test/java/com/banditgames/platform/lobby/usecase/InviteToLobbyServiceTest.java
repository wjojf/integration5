package com.banditgames.platform.lobby.usecase;



import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.domain.LobbyStatus;
import com.banditgames.platform.lobby.domain.LobbyVisibility;
import com.banditgames.platform.lobby.domain.events.LobbyInviteEvent;
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

class InviteToLobbyServiceTest {

    private LoadLobbyPort loadLobbyPort;
    private SaveLobbyPort saveLobbyPort;
    private PlatformEventPublisher publisher;
    private InviteToLobbyService service;

    @BeforeEach
    void setUp() {
        loadLobbyPort = mock(LoadLobbyPort.class);
        saveLobbyPort = mock(SaveLobbyPort.class);
        publisher = mock(PlatformEventPublisher.class);
        service = new InviteToLobbyService(loadLobbyPort, saveLobbyPort, publisher);
    }

    private Lobby privateLobby(UUID lobbyId, UUID hostId) {
        Lobby lobby = Lobby.builder()
                .id(lobbyId)
                .gameId(null)
                .hostId(hostId)
                .playerIds(new ArrayList<>())
                .status(LobbyStatus.WAITING)
                .maxPlayers(4)
                .visibility(LobbyVisibility.PRIVATE)
                .invitedPlayerIds(new ArrayList<>())
                .build();
        // seed host without join()
        lobby.getPlayerIds().add(hostId);
        return lobby;
    }

    @Test
    void invite_onlyHostCanInvite() {
        UUID lobbyId = UUID.randomUUID();
        UUID host = UUID.randomUUID();
        Lobby lobby = privateLobby(lobbyId, host);

        when(loadLobbyPort.findById(lobbyId)).thenReturn(Optional.of(lobby));

        assertThrows(
                com.banditgames.platform.lobby.domain.exception.LobbyOperationException.class,
                () -> service.inviteToLobby(lobbyId, UUID.randomUUID(), UUID.randomUUID())
        );

        verify(saveLobbyPort, never()).save(any());
        verify(publisher, never()).publish(any());
    }

    @Test
    void invite_saves_andPublishesEvent() {
        UUID lobbyId = UUID.randomUUID();
        UUID host = UUID.randomUUID();
        UUID invited = UUID.randomUUID();
        Lobby lobby = privateLobby(lobbyId, host);

        when(loadLobbyPort.findById(lobbyId)).thenReturn(Optional.of(lobby));
        when(saveLobbyPort.save(any(Lobby.class))).thenAnswer(inv -> inv.getArgument(0));

        Lobby saved = service.inviteToLobby(lobbyId, host, invited);

        assertTrue(saved.getInvitedPlayerIds().contains(invited));

        ArgumentCaptor<LobbyInviteEvent> captor = ArgumentCaptor.forClass(LobbyInviteEvent.class);
        verify(publisher).publish(captor.capture());
        assertEquals(lobbyId, captor.getValue().lobbyId());
        assertEquals(host, captor.getValue().hostId());
        assertEquals(invited, captor.getValue().invitedPlayerId());
    }
}

