package com.banditgames.platform.friends.usecase;

import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipAction;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import com.banditgames.platform.friends.domain.exception.FriendshipNotFoundException;
import com.banditgames.platform.friends.domain.exception.FriendshipOperationException;
import com.banditgames.platform.friends.port.in.PatchFriendRequestUseCase;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import com.banditgames.platform.friends.port.out.SaveFriendshipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcceptFriendRequestService implements PatchFriendRequestUseCase {

    private final LoadFriendshipPort loadFriendshipPort;
    private final SaveFriendshipPort saveFriendshipPort;

    @Override
    @Transactional
    public Friendship patchFriendRequest(UUID friendshipId, UUID userId, FriendshipAction action) {
        Friendship friendship = loadFriendshipPort.findById(friendshipId)
            .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found: " + friendshipId));

        if (!friendship.involvesUser(userId)) {
            throw new FriendshipOperationException("User is not part of this friendship");
        }

        try {
            switch (action) {
                case ACCEPT -> {
                    // Only addressee can accept a pending request
                    if (!friendship.isAddressee(userId)) {
                        throw new FriendshipOperationException("Only the addressee can accept a friend request");
                    }
                    friendship.accept();
                }
                case REJECT -> {
                    // Only addressee can reject a pending request
                    if (!friendship.isAddressee(userId)) {
                        throw new FriendshipOperationException("Only the addressee can reject a friend request");
                    }
                    friendship.reject();
                }
                case BLOCK -> {
                    // Can block from PENDING or ACCEPTED status
                    // Addressee can block requester, or either user can block in ACCEPTED status
                    if (friendship.getStatus() == FriendshipStatus.PENDING) {
                        // Only addressee can block from pending
                        if (!friendship.isAddressee(userId)) {
                            throw new FriendshipOperationException("Only the addressee can block a pending friend request");
                        }
                    } else if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                        // Either user can block an accepted friendship
                        // No additional check needed
                    } else {
                        throw new FriendshipOperationException("Cannot block from current status: " + friendship.getStatus());
                    }
                    friendship.block();
                }
                case CANCEL -> {
                    // Only requester can cancel a pending request
                    if (!friendship.isRequester(userId)) {
                        throw new FriendshipOperationException("Only the requester can cancel a friend request");
                    }
                    friendship.cancel(userId);
                }
                case REMOVE -> {
                    // Either user can remove an accepted friendship
                    friendship.remove(userId);
                }
                case UNBLOCK -> {
                    // Only the user who blocked (addressee) can unblock
                    if (!friendship.isAddressee(userId)) {
                        throw new FriendshipOperationException("Only the user who blocked can unblock");
                    }
                    friendship.unblock(userId);
                }
                default -> throw new FriendshipOperationException("Unknown action: " + action);
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new FriendshipOperationException(e.getMessage(), e);
        }

        friendship.setUpdatedAt(java.time.LocalDateTime.now());
        return saveFriendshipPort.save(friendship);
    }
}

