package com.banditgames.platform.friends.port.in;

import com.banditgames.platform.friends.domain.Friendship;

import java.util.UUID;

public interface RejectFriendRequestUseCase {
    Friendship rejectFriendRequest(UUID friendshipId, UUID userId);
}

