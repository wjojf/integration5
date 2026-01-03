package com.banditgames.platform.achievements.adapter.persistence;

import com.banditgames.platform.achievements.domain.Achievement;
import org.springframework.stereotype.Component;

@Component
public class AchievementMapper {

    public Achievement toDomain(AchievementEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return Achievement.builder()
                .id(entity.getId())
                .gameId(entity.getGameId())
                .name(entity.getName())
                .description(entity.getDescription())
                .triggerConditionString(entity.getTriggerConditionString())
                .thirdPartyAchievement(entity.getThirdPartyAchievement() != null ? entity.getThirdPartyAchievement() : false)
                .code(entity.getCode())
                .criteria(entity.getCriteria())
                .triggeringEventType(entity.getTriggeringEventType())
                .category(entity.getAchievementCategory())
                .rarity(entity.getAchievementRarity())
                .build();
    }

    public AchievementEntity toEntity(Achievement achievement) {
        if (achievement == null) {
            return null;
        }
        
        return AchievementEntity.builder()
                .id(achievement.getId())
                .gameId(achievement.getGameId())
                .name(achievement.getName())
                .description(achievement.getDescription())
                .triggerConditionString(achievement.getTriggerConditionString())
                .thirdPartyAchievement(achievement.getThirdPartyAchievement() != null ? achievement.getThirdPartyAchievement() : false)
                .code(achievement.getCode())
                .criteria(achievement.getCriteria())
                .triggeringEventType(achievement.getTriggeringEventType())
                .achievementCategory(achievement.getCategory())
                .achievementRarity(achievement.getRarity())
                .build();
    }
}

