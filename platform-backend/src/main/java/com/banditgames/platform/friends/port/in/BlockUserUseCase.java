package com.banditgames.platform.friends.port.in;

import com.banditgames.platform.friends.domain.Friendship;

import java.util.UUID;

public interface BlockUserUseCase {
    Friendship blockUser(UUID userId, UUID userToBlockId);
}

