package com.banditgames.platform.player.usecase;



import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetPlayerServiceTest {

    private LoadPlayerPort loadPlayerPort;
    private GetPlayerService service;

    @BeforeEach
    void setUp() {
        loadPlayerPort = mock(LoadPlayerPort.class);
        service = new GetPlayerService(loadPlayerPort);
    }

    @Test
    void getPlayer_delegatesToPort() {
        UUID id = UUID.randomUUID();
        Player player = Player.builder().playerId(id).username("Sean").build();
        when(loadPlayerPort.findById(id)).thenReturn(Optional.of(player));

        Optional<Player> result = service.getPlayer(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getPlayerId());
        verify(loadPlayerPort).findById(id);
    }

    @Test
    void getPlayer_returnsEmpty_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(loadPlayerPort.findById(id)).thenReturn(Optional.empty());

        Optional<Player> result = service.getPlayer(id);

        assertTrue(result.isEmpty());
        verify(loadPlayerPort).findById(id);
    }
}

