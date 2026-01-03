package com.banditgames.platform.achievements.port.in;

import com.banditgames.platform.achievements.domain.Achievement;

import java.util.List;
import java.util.UUID;

public interface GetAchievementsUseCase {
    List<Achievement> getAllAchievements(UUID gameId);
}

