package com.banditgames.platform.achievements.adapter.persistence;

import com.banditgames.platform.achievements.domain.UserAchievement;
import org.springframework.stereotype.Component;

@Component
public class UserAchievementMapper {

    public UserAchievement toDomain(UserAchievementEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return UserAchievement.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .achievementId(entity.getAchievement() != null ? entity.getAchievement().getId() : null)
                .unlockedAt(entity.getUnlockedAt())
                .build();
    }

    public UserAchievementEntity toEntity(UserAchievement userAchievement, AchievementEntity achievementEntity) {
        if (userAchievement == null) {
            return null;
        }
        
        return UserAchievementEntity.builder()
                .id(userAchievement.getId())
                .userId(userAchievement.getUserId())
                .achievement(achievementEntity)
                .unlockedAt(userAchievement.getUnlockedAt())
                .build();
    }
}

