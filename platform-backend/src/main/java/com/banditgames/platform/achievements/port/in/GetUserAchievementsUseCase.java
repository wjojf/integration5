package com.banditgames.platform.achievements.port.in;

import com.banditgames.platform.achievements.domain.UserAchievement;

import java.util.List;
import java.util.UUID;

public interface GetUserAchievementsUseCase {
    List<UserAchievement> getUserAchievements(UUID userId, UUID gameId);
}

