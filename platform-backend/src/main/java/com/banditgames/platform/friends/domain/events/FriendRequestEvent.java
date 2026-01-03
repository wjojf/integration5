package com.banditgames.platform.friends.domain.events;

import java.util.UUID;

public record FriendRequestEvent(
    UUID requesterId,
    UUID addresseeId
) {
}

