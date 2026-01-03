package com.banditgames.platform.friends.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {
    private UUID id;
    private UUID requesterId;
    private UUID addresseeId;
    private FriendshipStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Accepts a friend request.
     *
     * @throws IllegalStateException if the friendship is not in PENDING status
     */
    public void accept() {
        if (status != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Only pending friend requests can be accepted");
        }
        status = FriendshipStatus.ACCEPTED;
    }

    /**
     * Rejects a friend request.
     *
     * @throws IllegalStateException if the friendship is not in PENDING status
     */
    public void reject() {
        if (status != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Only pending friend requests can be rejected");
        }
        status = FriendshipStatus.REJECTED;
    }

    /**
     * Blocks a user.
     * Can be called from any status to block the other user.
     */
    public void block() {
        status = FriendshipStatus.BLOCKED;
    }

    /**
     * Cancels a pending friend request.
     * Only the requester can cancel their own pending request.
     *
     * @param userId The user ID attempting to cancel the request
     * @throws IllegalStateException if the friendship is not in PENDING status
     * @throws IllegalArgumentException if the user is not the requester
     */
    public void cancel(UUID userId) {
        if (status != FriendshipStatus.PENDING) {
            throw new IllegalStateException("Only pending friend requests can be cancelled");
        }
        if (!requesterId.equals(userId)) {
            throw new IllegalArgumentException("Only the requester can cancel a friend request");
        }
        status = FriendshipStatus.REJECTED;
    }

    /**
     * Removes an accepted friend (ends the friendship).
     * Either user can remove an accepted friendship.
     *
     * @param userId The user ID attempting to remove the friendship
     * @throws IllegalStateException if the friendship is not in ACCEPTED status
     * @throws IllegalArgumentException if the user is not part of this friendship
     */
    public void remove(UUID userId) {
        if (status != FriendshipStatus.ACCEPTED) {
            throw new IllegalStateException("Only accepted friendships can be removed");
        }
        if (!involvesUser(userId)) {
            throw new IllegalArgumentException("User is not part of this friendship");
        }
        status = FriendshipStatus.REJECTED;
    }

    /**
     * Unblocks a blocked user.
     * Only the user who blocked can unblock.
     *
     * @param userId The user ID attempting to unblock
     * @throws IllegalStateException if the friendship is not in BLOCKED status
     * @throws IllegalArgumentException if the user is not the addressee (the one who blocked)
     */
    public void unblock(UUID userId) {
        if (status != FriendshipStatus.BLOCKED) {
            throw new IllegalStateException("Only blocked friendships can be unblocked");
        }
        // The addressee is the one who can block/unblock
        if (!addresseeId.equals(userId)) {
            throw new IllegalArgumentException("Only the user who blocked can unblock");
        }
        status = FriendshipStatus.REJECTED;
    }

    /**
     * Checks if the friendship is active (accepted).
     */
    public boolean isActive() {
        return status == FriendshipStatus.ACCEPTED;
    }

    /**
     * Checks if the user is the requester.
     */
    public boolean isRequester(UUID userId) {
        return requesterId.equals(userId);
    }

    /**
     * Checks if the user is the addressee.
     */
    public boolean isAddressee(UUID userId) {
        return addresseeId.equals(userId);
    }

    /**
     * Checks if a user is part of this friendship.
     */
    public boolean involvesUser(UUID userId) {
        return requesterId.equals(userId) || addresseeId.equals(userId);
    }

    /**
     * Gets the other user in the friendship.
     */
    public UUID getOtherUser(UUID userId) {
        if (requesterId.equals(userId)) {
            return addresseeId;
        } else if (addresseeId.equals(userId)) {
            return requesterId;
        }
        throw new IllegalArgumentException("User is not part of this friendship");
    }
}
