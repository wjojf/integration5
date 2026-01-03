package com.banditgames.platform.friends.usecase;

import com.banditgames.platform.acl.port.out.PlayerContextPort;
import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import com.banditgames.platform.friends.domain.events.FriendRequestEvent;
import com.banditgames.platform.friends.port.in.SendFriendRequestUseCase;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import com.banditgames.platform.friends.port.out.SaveFriendshipPort;
import com.banditgames.platform.shared.events.PlatformEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SendFriendRequestService implements SendFriendRequestUseCase {

    private final LoadFriendshipPort loadFriendshipPort;
    private final SaveFriendshipPort saveFriendshipPort;
    private final PlatformEventPublisher eventPublisher;
    private final PlayerContextPort playerContextPort;

    @Override
    @Transactional
    public Friendship sendFriendRequest(UUID requesterId, UUID addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        // Validate that the addressee player exists
        if (!playerContextPort.playerExists(addresseeId)) {
            throw new IllegalArgumentException("Player not found");
        }

        // Check if friendship already exists
        if (loadFriendshipPort.existsByRequesterIdAndAddresseeId(requesterId, addresseeId) ||
                loadFriendshipPort.existsByRequesterIdAndAddresseeId(addresseeId, requesterId)) {
            throw new IllegalStateException("Friendship already exists");
        }

        Friendship friendship = Friendship.builder()
                .requesterId(requesterId)
                .addresseeId(addresseeId)
                .status(FriendshipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Friendship saved = saveFriendshipPort.save(friendship);

        // Publish event
        eventPublisher.publish(new FriendRequestEvent(
                requesterId,
                addresseeId));

        return saved;
    }
}
