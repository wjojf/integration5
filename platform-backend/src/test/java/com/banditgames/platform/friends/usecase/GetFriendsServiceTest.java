package com.banditgames.platform.friends.usecase;



import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class GetFriendsServiceTest {

    private LoadFriendshipPort loadFriendshipPort;
    private GetFriendsService service;

    @BeforeEach
    void setUp() {
        loadFriendshipPort = mock(LoadFriendshipPort.class);
        service = new GetFriendsService(loadFriendshipPort);
    }

    @Test
    void pending_callsFindPendingFriendRequests() {
        UUID userId = UUID.randomUUID();
        List<Friendship> expected = List.of();
        when(loadFriendshipPort.findPendingFriendRequests(userId)).thenReturn(expected);

        List<Friendship> result = service.getFriendsByStatus(userId, FriendshipStatus.PENDING);

        assertSame(expected, result);
        verify(loadFriendshipPort).findPendingFriendRequests(userId);
    }

    @Test
    void accepted_callsFindAcceptedFriendshipsByUserId() {
        UUID userId = UUID.randomUUID();
        List<Friendship> expected = List.of();
        when(loadFriendshipPort.findAcceptedFriendshipsByUserId(userId)).thenReturn(expected);

        List<Friendship> result = service.getFriendsByStatus(userId, FriendshipStatus.ACCEPTED);

        assertSame(expected, result);
        verify(loadFriendshipPort).findAcceptedFriendshipsByUserId(userId);
    }
}

