package com.banditgames.platform.friends.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

 class FriendshipTest {

    @Test
    void accept_pending_becomesAccepted() {
        Friendship f = Friendship.builder()
                .requesterId(UUID.randomUUID())
                .addresseeId(UUID.randomUUID())
                .status(FriendshipStatus.PENDING)
                .build();

        f.accept();

        assertEquals(FriendshipStatus.ACCEPTED, f.getStatus());
        assertTrue(f.isActive());
    }

    @Test
    void accept_nonPending_throws() {
        Friendship f = Friendship.builder()
                .requesterId(UUID.randomUUID())
                .addresseeId(UUID.randomUUID())
                .status(FriendshipStatus.ACCEPTED)
                .build();

        assertThrows(IllegalStateException.class, f::accept);
    }

    @Test
    void reject_pending_becomesRejected() {
        Friendship f = Friendship.builder()
                .requesterId(UUID.randomUUID())
                .addresseeId(UUID.randomUUID())
                .status(FriendshipStatus.PENDING)
                .build();

        f.reject();

        assertEquals(FriendshipStatus.REJECTED, f.getStatus());
        assertFalse(f.isActive());
    }

    @Test
    void block_setsBlocked() {
        Friendship f = Friendship.builder()
                .requesterId(UUID.randomUUID())
                .addresseeId(UUID.randomUUID())
                .status(FriendshipStatus.PENDING)
                .build();

        f.block();

        assertEquals(FriendshipStatus.BLOCKED, f.getStatus());
    }

    @Test
    void involvesUser_true_forRequesterOrAddressee() {
        UUID requester = UUID.randomUUID();
        UUID addressee = UUID.randomUUID();
        Friendship f = Friendship.builder()
                .requesterId(requester)
                .addresseeId(addressee)
                .status(FriendshipStatus.PENDING)
                .build();

        assertTrue(f.involvesUser(requester));
        assertTrue(f.involvesUser(addressee));
        assertFalse(f.involvesUser(UUID.randomUUID()));
    }

    @Test
    void getOtherUser_returnsOtherOrThrows() {
        UUID requester = UUID.randomUUID();
        UUID addressee = UUID.randomUUID();
        Friendship f = Friendship.builder()
                .requesterId(requester)
                .addresseeId(addressee)
                .status(FriendshipStatus.PENDING)
                .build();

        assertEquals(addressee, f.getOtherUser(requester));
        assertEquals(requester, f.getOtherUser(addressee));
        assertThrows(IllegalArgumentException.class, () -> f.getOtherUser(UUID.randomUUID()));
    }
}
