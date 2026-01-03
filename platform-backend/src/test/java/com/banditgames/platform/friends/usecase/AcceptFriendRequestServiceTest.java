package com.banditgames.platform.friends.usecase;



import com.banditgames.platform.friends.domain.*;
import com.banditgames.platform.friends.domain.exception.FriendshipNotFoundException;
import com.banditgames.platform.friends.domain.exception.FriendshipOperationException;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import com.banditgames.platform.friends.port.out.SaveFriendshipPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcceptFriendRequestServiceTest {

    private LoadFriendshipPort loadFriendshipPort;
    private SaveFriendshipPort saveFriendshipPort;
    private AcceptFriendRequestService service;

    @BeforeEach
    void setUp() {
        loadFriendshipPort = mock(LoadFriendshipPort.class);
        saveFriendshipPort = mock(SaveFriendshipPort.class);
        service = new AcceptFriendRequestService(loadFriendshipPort, saveFriendshipPort);
    }

    @Test
    void patchFriendRequest_notFound_throws() {
        UUID friendshipId = UUID.randomUUID();
        when(loadFriendshipPort.findById(friendshipId)).thenReturn(Optional.empty());

        assertThrows(FriendshipNotFoundException.class,
                () -> service.patchFriendRequest(friendshipId, UUID.randomUUID(), FriendshipAction.ACCEPT));
    }

    @Test
    void patchFriendRequest_onlyAddresseeCanAct_throws() {
        UUID friendshipId = UUID.randomUUID();
        UUID requester = UUID.randomUUID();
        UUID addressee = UUID.randomUUID();

        Friendship f = Friendship.builder()
                .id(friendshipId)
                .requesterId(requester)
                .addresseeId(addressee)
                .status(FriendshipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadFriendshipPort.findById(friendshipId)).thenReturn(Optional.of(f));

        assertThrows(FriendshipOperationException.class,
                () -> service.patchFriendRequest(friendshipId, requester, FriendshipAction.ACCEPT));

        verify(saveFriendshipPort, never()).save(any());
    }

    @Test
    void patchFriendRequest_accept_pending_savesAccepted() {
        UUID friendshipId = UUID.randomUUID();
        UUID requester = UUID.randomUUID();
        UUID addressee = UUID.randomUUID();

        Friendship f = Friendship.builder()
                .id(friendshipId)
                .requesterId(requester)
                .addresseeId(addressee)
                .status(FriendshipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadFriendshipPort.findById(friendshipId)).thenReturn(Optional.of(f));
        when(saveFriendshipPort.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

        Friendship saved = service.patchFriendRequest(friendshipId, addressee, FriendshipAction.ACCEPT);

        assertEquals(FriendshipStatus.ACCEPTED, saved.getStatus());
        verify(saveFriendshipPort).save(any(Friendship.class));
    }

    @Test
    void patchFriendRequest_reject_nonPending_mapsToFriendshipOperationException() {
        UUID friendshipId = UUID.randomUUID();
        UUID requester = UUID.randomUUID();
        UUID addressee = UUID.randomUUID();

        Friendship f = Friendship.builder()
                .id(friendshipId)
                .requesterId(requester)
                .addresseeId(addressee)
                .status(FriendshipStatus.ACCEPTED) // not pending -> reject() throws IllegalStateException
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(loadFriendshipPort.findById(friendshipId)).thenReturn(Optional.of(f));

        assertThrows(FriendshipOperationException.class,
                () -> service.patchFriendRequest(friendshipId, addressee, FriendshipAction.REJECT));

        verify(saveFriendshipPort, never()).save(any());
    }
}

