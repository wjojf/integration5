package com.banditgames.platform.player.usecase;



import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.exception.PlayerNotFoundException;
import com.banditgames.platform.player.port.out.DeletePlayerPort;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DeletePlayerServiceTest {

    private LoadPlayerPort loadPlayerPort;
    private DeletePlayerPort deletePlayerPort;
    private DeletePlayerService service;

    @BeforeEach
    void setUp() {
        loadPlayerPort = mock(LoadPlayerPort.class);
        deletePlayerPort = mock(DeletePlayerPort.class);
        service = new DeletePlayerService(loadPlayerPort, deletePlayerPort);
    }

    @Test
    void deletePlayer_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(loadPlayerPort.findById(id)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class, () -> service.deletePlayer(id));

        verify(deletePlayerPort, never()).deleteById(any());
    }

    @Test
    void deletePlayer_deletesWhenFound() {
        UUID id = UUID.randomUUID();
        when(loadPlayerPort.findById(id)).thenReturn(Optional.of(Player.builder().playerId(id).build()));

        service.deletePlayer(id);

        verify(deletePlayerPort).deleteById(id);
    }
}

