package com.banditgames.platform.achievements.port.out;

import com.banditgames.platform.achievements.domain.Achievement;

public interface SaveAchievementPort {
    Achievement save(Achievement achievement);
}
