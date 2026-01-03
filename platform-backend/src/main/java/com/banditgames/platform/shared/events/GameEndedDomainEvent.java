package com.banditgames.platform.shared.events;

import java.util.UUID;

/**
 * Shared domain event published when a game ends.
 * This event can be consumed by any module that needs to react to game completion.
 */
public record GameEndedDomainEvent(
    UUID lobbyId,
    UUID winnerId
) {
}

