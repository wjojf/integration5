package com.banditgames.platform.friends.port.in;

import com.banditgames.platform.friends.domain.Friendship;

import java.util.UUID;

public interface SendFriendRequestUseCase {
    Friendship sendFriendRequest(UUID requesterId, UUID addresseeId);
}

