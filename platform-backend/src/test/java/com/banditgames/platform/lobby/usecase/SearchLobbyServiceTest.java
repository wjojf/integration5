package com.banditgames.platform.lobby.usecase;

import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.port.out.LoadLobbyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class SearchLobbyServiceTest {

    private LoadLobbyPort loadLobbyPort;
    private SearchLobbyService service;

    @BeforeEach
    void setUp() {
        loadLobbyPort = mock(LoadLobbyPort.class);
        service = new SearchLobbyService(loadLobbyPort);
    }

    @Test
    void searchLobbies_delegatesToPort() {
        UUID gameId = UUID.randomUUID();
        List<UUID> hostIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 20);
        Page<Lobby> expected = new PageImpl<>(List.of());

        when(loadLobbyPort.searchLobbies(gameId, hostIds, pageable)).thenReturn(expected);

        Page<Lobby> result = service.searchLobbies(gameId, hostIds, pageable);

        assertSame(expected, result);
        verify(loadLobbyPort).searchLobbies(gameId, hostIds, pageable);
    }
}

