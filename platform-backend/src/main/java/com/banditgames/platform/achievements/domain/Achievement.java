package com.banditgames.platform.achievements.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {
    private UUID id;
    private UUID gameId;
    private String name;
    private String description; // Brief explanation of achievement
    private String triggerConditionString; // Exact explanation of how to achieve achievement

    // Third party achievements operate on code since they handle their own achievements events
    @Builder.Default
    private Boolean thirdPartyAchievement = false;
    private String code;


    private AchievementCriteria criteria;
    private TriggeringEventType triggeringEventType;

    private AchievementCategory category;
    private AchievementRarity rarity;
}

