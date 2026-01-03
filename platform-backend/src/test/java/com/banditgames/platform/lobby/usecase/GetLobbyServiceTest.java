package com.banditgames.platform.lobby.usecase;



import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetLobbyServiceTest {

    private LoadLobbyPort loadLobbyPort;
    private GetLobbyService service;

    @BeforeEach
    void setUp() {
        loadLobbyPort = mock(LoadLobbyPort.class);
        service = new GetLobbyService(loadLobbyPort);
    }

    @Test
    void getLobby_delegatesToPort() {
        UUID id = UUID.randomUUID();
        when(loadLobbyPort.findById(id)).thenReturn(Optional.empty());

        Optional<Lobby> result = service.getLobby(id);

        assertTrue(result.isEmpty());
        verify(loadLobbyPort).findById(id);
    }
}

