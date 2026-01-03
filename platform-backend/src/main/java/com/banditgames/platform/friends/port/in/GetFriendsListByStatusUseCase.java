package com.banditgames.platform.friends.port.in;

import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;

import java.util.List;
import java.util.UUID;

public interface GetFriendsListByStatusUseCase {
    List<Friendship> getFriendsByStatus(UUID userId, FriendshipStatus status);

    List<Friendship> searchFriendsByStatus(UUID userId, FriendshipStatus status);
}

