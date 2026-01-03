package com.banditgames.platform.lobby.domain;



import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LobbyTest {

    private Lobby baseLobby(LobbyVisibility visibility, int maxPlayers, LobbyStatus status) {
        UUID host = UUID.randomUUID();
        Lobby lobby = Lobby.builder()
                .id(UUID.randomUUID())
                .gameId(null)
                .hostId(host)
                .playerIds(new ArrayList<>())
                .status(status)
                .maxPlayers(maxPlayers)
                .visibility(visibility)
                .invitedPlayerIds(new ArrayList<>())
                .build();

        lobby.getPlayerIds().add(host);
        return lobby;
    }

    @Test
    void join_addsPlayer_whenValid() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.WAITING);
        UUID p2 = UUID.randomUUID();

        lobby.join(p2);

        assertTrue(lobby.getPlayerIds().contains(p2));
        assertEquals(2, lobby.getPlayerIds().size());
    }

    @Test
    void join_whenStarted_throws() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.STARTED);

        assertThrows(IllegalStateException.class, () -> lobby.join(UUID.randomUUID()));
    }

    @Test
    void join_whenCancelled_throws() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.CANCELLED);

        assertThrows(IllegalStateException.class, () -> lobby.join(UUID.randomUUID()));
    }

    @Test
    void join_whenFull_throws() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 1, LobbyStatus.WAITING); // host already occupies 1 slot

        assertThrows(IllegalStateException.class, () -> lobby.join(UUID.randomUUID()));
    }

    @Test
    void join_samePlayerTwice_throws() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.WAITING);
        UUID p2 = UUID.randomUUID();
        lobby.join(p2);

        assertThrows(IllegalStateException.class, () -> lobby.join(p2));
    }

    @Test
    void privateLobby_joinWithoutInvite_throws() {
        Lobby lobby = baseLobby(LobbyVisibility.PRIVATE, 4, LobbyStatus.WAITING);

        assertThrows(IllegalStateException.class, () -> lobby.join(UUID.randomUUID()));
    }

    @Test
    void privateLobby_invite_thenJoin_succeeds() {
        Lobby lobby = baseLobby(LobbyVisibility.PRIVATE, 4, LobbyStatus.WAITING);
        UUID invited = UUID.randomUUID();

        lobby.invite(invited);
        assertTrue(lobby.getInvitedPlayerIds().contains(invited));

        lobby.join(invited);
        assertTrue(lobby.getPlayerIds().contains(invited));
    }

    @Test
    void leave_nonMember_throws() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.WAITING);

        assertThrows(IllegalStateException.class, () -> lobby.leave(UUID.randomUUID()));
    }

    @Test
    void leave_removesPlayer_andRemovesInviteIfPresent() {
        Lobby lobby = baseLobby(LobbyVisibility.PRIVATE, 4, LobbyStatus.WAITING);
        UUID invited = UUID.randomUUID();
        lobby.invite(invited);
        lobby.join(invited);

        lobby.leave(invited);

        assertFalse(lobby.getPlayerIds().contains(invited));
        assertFalse(lobby.getInvitedPlayerIds().contains(invited));
    }

    @Test
    void leave_hostCancelsLobby_ifNotStarted() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.WAITING);
        UUID host = lobby.getHostId();

        lobby.leave(host);

        assertEquals(LobbyStatus.CANCELLED, lobby.getStatus());
    }

    @Test
    void start_requiresGameId_players_andNotCancelledOrStarted() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.WAITING);

        // no gameId -> should throw
        assertThrows(IllegalStateException.class, lobby::start);

        // set gameId then start works
        lobby.setGameId(UUID.randomUUID());
        lobby.start();

        assertEquals(LobbyStatus.STARTED, lobby.getStatus());
        assertNotNull(lobby.getStartedAt());
    }

    @Test
    void complete_onlyWhenStarted() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.WAITING);

        assertThrows(IllegalStateException.class, lobby::complete);

        lobby.setGameId(UUID.randomUUID());
        lobby.start();
        lobby.complete();

        assertEquals(LobbyStatus.COMPLETED, lobby.getStatus());
    }

    @Test
    void invite_onlyPrivate_notStarted_notCancelled_notAlreadyInLobby_notAlreadyInvited() {
        Lobby publicLobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.WAITING);
        assertThrows(IllegalStateException.class, () -> publicLobby.invite(UUID.randomUUID()));

        Lobby privateLobby = baseLobby(LobbyVisibility.PRIVATE, 4, LobbyStatus.WAITING);
        UUID p = UUID.randomUUID();

        privateLobby.invite(p);
        assertTrue(privateLobby.getInvitedPlayerIds().contains(p));

        assertThrows(IllegalStateException.class, () -> privateLobby.invite(p)); // already invited
        assertThrows(IllegalStateException.class, () -> privateLobby.invite(privateLobby.getHostId())); // already in lobby
    }

    @Test
    void canJoin_matchesRules() {
        Lobby lobby = baseLobby(LobbyVisibility.PUBLIC, 2, LobbyStatus.WAITING);
        UUID p2 = UUID.randomUUID();

        assertTrue(lobby.canJoin(p2));
        lobby.join(p2);
        assertFalse(lobby.canJoin(UUID.randomUUID())); // now full

        lobby = baseLobby(LobbyVisibility.PUBLIC, 4, LobbyStatus.CANCELLED);
        assertFalse(lobby.canJoin(UUID.randomUUID()));
    }
}

