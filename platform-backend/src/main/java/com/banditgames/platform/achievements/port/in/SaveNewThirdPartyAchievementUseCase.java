package com.banditgames.platform.achievements.port.in;

import com.banditgames.platform.achievements.domain.Achievement;

import java.util.UUID;

public interface SaveNewThirdPartyAchievementUseCase {

    Achievement SaveNewThirdPartyAchievement(SaveNewThirdPartyAchievementRecord record);

    record SaveNewThirdPartyAchievementRecord(UUID gameId,
                                              String name,
                                              String description,
                                              String triggeringConditionString,
                                              Boolean thirdParty,
                                              String code) {
    }
}
