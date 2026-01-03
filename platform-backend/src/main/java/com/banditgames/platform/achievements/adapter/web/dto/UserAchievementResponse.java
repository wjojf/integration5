package com.banditgames.platform.achievements.adapter.web.dto;

import com.banditgames.platform.achievements.domain.UserAchievement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "User achievement information")
public class UserAchievementResponse {
    
    @Schema(description = "Unique identifier of the user achievement record", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "Unique identifier of the user who unlocked this achievement", example = "660e8400-e29b-41d4-a716-446655440001")
    private String userId;
    
    @Schema(description = "Unique identifier of the achievement", example = "770e8400-e29b-41d4-a716-446655440002")
    private UUID achievementId;
    
    @Schema(description = "Date and time when the achievement was unlocked", example = "2024-01-15T10:30:00")
    private LocalDateTime unlockedAt;

    public static UserAchievementResponse fromDomain(UserAchievement userAchievement) {
        return UserAchievementResponse.builder()
                .id(userAchievement.getId())
                .userId(userAchievement.getUserId())
                .achievementId(userAchievement.getAchievementId())
                .unlockedAt(userAchievement.getUnlockedAt())
                .build();
    }
}

