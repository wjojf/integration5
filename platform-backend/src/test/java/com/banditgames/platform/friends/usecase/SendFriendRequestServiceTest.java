package com.banditgames.platform.friends.usecase;

import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import com.banditgames.platform.friends.domain.events.FriendRequestEvent;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import com.banditgames.platform.friends.port.out.SaveFriendshipPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SendFriendRequestServiceTest {

    private LoadFriendshipPort loadFriendshipPort;
    private SaveFriendshipPort saveFriendshipPort;
    private PlatformEventPublisher eventPublisher;
    private SendFriendRequestService service;

    @BeforeEach
    void setUp() {
        loadFriendshipPort = mock(LoadFriendshipPort.class);
        saveFriendshipPort = mock(SaveFriendshipPort.class);
        eventPublisher = mock(PlatformEventPublisher.class);
        service = new SendFriendRequestService(loadFriendshipPort, saveFriendshipPort, eventPublisher);
    }

    @Test
    void sendFriendRequest_toYourself_throws() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> service.sendFriendRequest(id, id));
        verifyNoInteractions(loadFriendshipPort, saveFriendshipPort, eventPublisher);
    }

    @Test
    void sendFriendRequest_whenAlreadyExists_throws() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        when(loadFriendshipPort.existsByRequesterIdAndAddresseeId(a, b)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.sendFriendRequest(a, b));

        verify(saveFriendshipPort, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void sendFriendRequest_createsPending_saves_andPublishesEvent() {
        UUID requester = UUID.randomUUID();
        UUID addressee = UUID.randomUUID();

        when(loadFriendshipPort.existsByRequesterIdAndAddresseeId(requester, addressee)).thenReturn(false);
        when(loadFriendshipPort.existsByRequesterIdAndAddresseeId(addressee, requester)).thenReturn(false);

        // Return what was saved (simulate persistence)
        when(saveFriendshipPort.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

        Friendship saved = service.sendFriendRequest(requester, addressee);

        assertEquals(requester, saved.getRequesterId());
        assertEquals(addressee, saved.getAddresseeId());
        assertEquals(FriendshipStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        ArgumentCaptor<FriendRequestEvent> eventCaptor = ArgumentCaptor.forClass(FriendRequestEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(requester, eventCaptor.getValue().requesterId());
        assertEquals(addressee, eventCaptor.getValue().addresseeId());
    }
}
