package com.banditgames.platform.player.usecase;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.Rank;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SearchPlayersServiceTest {

    private LoadPlayerPort loadPlayerPort;
    private SearchPlayersService service;

    @BeforeEach
    void setUp() {
        loadPlayerPort = mock(LoadPlayerPort.class);
        service = new SearchPlayersService(loadPlayerPort);
    }

    @Test
    void searchPlayers_delegatesToPort() {
        String username = "sean";
        Rank rank = Rank.BRONZE;
        Pageable pageable = Pageable.unpaged();

        List<Player> expected = List.of(Player.builder().playerId(UUID.randomUUID()).username("sean").build());

        Page<Player> expectedPage = new PageImpl<>(expected, pageable, expected.size());
        when(loadPlayerPort.searchPlayers(username, rank, pageable)).thenReturn(expectedPage);

        Page<Player> resultPage = service.searchPlayers(username, rank, pageable);

        assertEquals(expected, resultPage.getContent());
        verify(loadPlayerPort).searchPlayers(username, rank, pageable);
    }
}
