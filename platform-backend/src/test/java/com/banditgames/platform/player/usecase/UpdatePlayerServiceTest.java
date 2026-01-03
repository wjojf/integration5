package com.banditgames.platform.player.usecase;



import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.exception.PlayerNotFoundException;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import com.banditgames.platform.player.port.out.SavePlayerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UpdatePlayerServiceTest {

    private LoadPlayerPort loadPlayerPort;
    private SavePlayerPort savePlayerPort;
    private UpdatePlayerService service;

    @BeforeEach
    void setUp() {
        loadPlayerPort = mock(LoadPlayerPort.class);
        savePlayerPort = mock(SavePlayerPort.class);
        service = new UpdatePlayerService(loadPlayerPort, savePlayerPort);
    }

    @Test
    void updatePlayer_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(loadPlayerPort.findById(id)).thenReturn(Optional.empty());

        assertThrows(PlayerNotFoundException.class,
                () -> service.updatePlayer(id, "new", "bio", "addr", List.of(UUID.randomUUID())));

        verify(savePlayerPort, never()).save(any());
    }

    @Test
    void updatePlayer_updatesOnlyNonNullFields_andSaves() {
        UUID id = UUID.randomUUID();
        Player existing = Player.builder()
                .playerId(id)
                .username("oldName")
                .bio("oldBio")
                .address("oldAddr")
                .gamePreferences(List.of(UUID.randomUUID()))
                .build();

        when(loadPlayerPort.findById(id)).thenReturn(Optional.of(existing));
        when(savePlayerPort.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        List<UUID> newPrefs = List.of(UUID.randomUUID(), UUID.randomUUID());

        Player saved = service.updatePlayer(id, "newName", null, "newAddr", newPrefs);

        assertEquals("newName", saved.getUsername());
        assertEquals("oldBio", saved.getBio());          // unchanged because null
        assertEquals("newAddr", saved.getAddress());
        assertEquals(newPrefs, saved.getGamePreferences());

        verify(savePlayerPort).save(existing);
    }

    @Test
    void updatePlayer_allNull_doesNotChangeAnything_butStillSaves() {
        UUID id = UUID.randomUUID();
        Player existing = Player.builder()
                .playerId(id)
                .username("name")
                .bio("bio")
                .address("addr")
                .gamePreferences(List.of(UUID.randomUUID()))
                .build();

        when(loadPlayerPort.findById(id)).thenReturn(Optional.of(existing));
        when(savePlayerPort.save(any(Player.class))).thenAnswer(inv -> inv.getArgument(0));

        Player saved = service.updatePlayer(id, null, null, null, null);

        assertEquals("name", saved.getUsername());
        assertEquals("bio", saved.getBio());
        assertEquals("addr", saved.getAddress());
        assertNotNull(saved.getGamePreferences());

        verify(savePlayerPort).save(existing);
    }
}

