package com.banditgames.platform.friends.port.out;

import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadFriendshipPort {
    Optional<Friendship> findById(UUID friendshipId);
    Optional<Friendship> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);
    List<Friendship> findAcceptedFriendshipsByUserId(UUID userId);
    List<Friendship> findPendingFriendRequests(UUID userId);
    List<Friendship> findBlockedFriendRequestsByUserId(UUID userId);
    List<Friendship> findRejectedFriendRequestsByUserId(UUID userId);
    List<Friendship> finedFriendRequestsByUserIdAndStatus(UUID userId, FriendshipStatus status);
    Page<Friendship> findFriendRequestsByUserIdAndStatus(UUID userId, FriendshipStatus status, Pageable pageable);
    boolean existsByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);
}

