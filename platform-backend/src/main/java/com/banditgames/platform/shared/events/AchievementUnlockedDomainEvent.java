package com.banditgames.platform.shared.events;

import java.util.UUID;

/**
 * Shared domain event published when an achievement is unlocked.
 * This event can be consumed by any module that needs to react to achievement unlocks.
 */
public record AchievementUnlockedDomainEvent(
    UUID playerId,
    String achievementId
) {
}

