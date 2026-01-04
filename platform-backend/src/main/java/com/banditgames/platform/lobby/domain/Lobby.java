package com.banditgames.platform.lobby.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lobby {
    @Setter
    private UUID id;
    @Setter
    private UUID gameId;
    @Setter
    private UUID sessionId;
    private UUID hostId;
    @Setter
    private String name;
    @Setter
    private String description;
    @Builder.Default
    private List<UUID> playerIds = new ArrayList<>();
    @Setter
    private LobbyStatus status;
    private Integer maxPlayers;
    private LobbyVisibility visibility;
    @Builder.Default
    private List<UUID> invitedPlayerIds = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;

    /**
     * Validates and adds a player to the lobby.
     *
     * @param playerId The player ID to add
     * @throws IllegalStateException if the lobby is full, already started, or
     *                               player is already in the lobby
     */
    public void join(UUID playerId) {
        // Cannot join if lobby already started, in progress, or cancelled
        if (status == LobbyStatus.STARTED || status == LobbyStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot join a lobby that has already started");
        }
        if (status == LobbyStatus.CANCELLED) {
            throw new IllegalStateException("Cannot join a cancelled lobby");
        }
        // Full capacity check
        if (playerIds.size() >= maxPlayers) {
            throw new IllegalStateException("Lobby is full. Maximum players: " + maxPlayers);
        }
        // Private lobby requires invitation
        if (visibility == LobbyVisibility.PRIVATE && !invitedPlayerIds.contains(playerId)) {
            throw new IllegalStateException("Cannot join a private lobby without an invitation");
        }
        // Prevent duplicate joins
        if (playerIds.contains(playerId)) {
            throw new IllegalStateException("Player is already in the lobby");
        }
        playerIds.add(playerId);
    }

    /**
     * Removes a player from the lobby.
     * This method is idempotent - if the player is not in the lobby, it returns without error.
     *
     * @param playerId The player ID to remove
     */
    public void leave(UUID playerId) {
        // Idempotent operation: if player is not in lobby, just return
        if (!playerIds.contains(playerId)) {
            return;
        }
        
        playerIds.remove(playerId);
        invitedPlayerIds.remove(playerId);

        // If host leaves, cancel the lobby (unless game is in progress)
        if (hostId.equals(playerId) && status != LobbyStatus.STARTED && status != LobbyStatus.IN_PROGRESS) {
            status = LobbyStatus.CANCELLED;
        }
    }

    /**
     * Starts the lobby when a game is selected.
     *
     * @throws IllegalStateException if the lobby cannot be started
     */
    public void start() {
        validateCanStart();
        status = LobbyStatus.STARTED;
        startedAt = LocalDateTime.now();
    }

    /**
     * Completes the lobby after the game has ended.
     */
    public void complete() {
        if (status != LobbyStatus.STARTED && status != LobbyStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete a lobby that hasn't been started");
        }
        status = LobbyStatus.COMPLETED;
    }

    /**
     * Resets the lobby after a game ends, clearing the session ID, game ID, and resetting status to WAITING
     * so that a new game can be started in the same lobby.
     */
    public void resetAfterGameEnd() {
        if (status == LobbyStatus.STARTED || status == LobbyStatus.IN_PROGRESS) {
            sessionId = null;
            gameId = null;  // Clear gameId so players can select a new game
            status = LobbyStatus.WAITING;
        }
    }

    /**
     * Invites a friend to the lobby.
     *
     * @param playerId The player ID to invite
     * @throws IllegalStateException if the lobby is not private or cannot accept
     *                               invites
     */
    public void invite(UUID playerId) {
        validateCanInvite(playerId);
        if (!invitedPlayerIds.contains(playerId)) {
            invitedPlayerIds.add(playerId);
        }
    }

    /**
     * Checks if a player can join the lobby.
     */
    public boolean canJoin(UUID playerId) {
        if (status == LobbyStatus.STARTED || status == LobbyStatus.IN_PROGRESS || status == LobbyStatus.CANCELLED) {
            return false;
        }
        if (playerIds.size() >= maxPlayers) {
            return false;
        }
        if (playerIds.contains(playerId)) {
            return false;
        }
        if (visibility == LobbyVisibility.PRIVATE && !invitedPlayerIds.contains(playerId)) {
            return false;
        }
        return true;
    }

    private void validateCanJoin(UUID playerId) {
        if (status == LobbyStatus.STARTED || status == LobbyStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot join a lobby that has already started");
        }
        if (status == LobbyStatus.CANCELLED) {
            throw new IllegalStateException("Cannot join a cancelled lobby");
        }
        if (playerIds.size() >= maxPlayers) {
            throw new IllegalStateException("Lobby is full. Maximum players: " + maxPlayers);
        }
        if (playerIds.contains(playerId)) {
            throw new IllegalStateException("Player is already in the lobby");
        }
        if (visibility == LobbyVisibility.PRIVATE && !invitedPlayerIds.contains(playerId)) {
            throw new IllegalStateException("Cannot join a private lobby without an invitation");
        }
    }


    private void validateCanStart() {
        if (status == LobbyStatus.STARTED || status == LobbyStatus.IN_PROGRESS) {
            throw new IllegalStateException("Lobby has already been started");
        }
        if (status == LobbyStatus.CANCELLED) {
            throw new IllegalStateException("Cannot start a cancelled lobby");
        }
        if (gameId == null) {
            throw new IllegalStateException("Cannot start a lobby without a game selected");
        }
        if (playerIds.isEmpty()) {
            throw new IllegalStateException("Cannot start a lobby without players");
        }
    }

    private void validateCanInvite(UUID playerId) {
        if (visibility != LobbyVisibility.PRIVATE) {
            throw new IllegalStateException("Can only invite players to private lobbies");
        }
        if (status == LobbyStatus.STARTED || status == LobbyStatus.IN_PROGRESS || status == LobbyStatus.CANCELLED) {
            throw new IllegalStateException("Cannot invite players to a started or cancelled lobby");
        }
        if (playerIds.contains(playerId)) {
            throw new IllegalStateException("Player is already in the lobby");
        }
        if (invitedPlayerIds.contains(playerId)) {
            throw new IllegalStateException("Player has already been invited");
        }
    }
}
