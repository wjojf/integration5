package com.banditgames.platform.friends.port.in;

import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipAction;

import java.util.UUID;

public interface PatchFriendRequestUseCase {
    Friendship patchFriendRequest(UUID friendshipId, UUID userId, FriendshipAction action);
}

