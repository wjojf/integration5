package com.banditgames.platform.achievements.adapter.web.dto;

import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.domain.AchievementCategory;
import com.banditgames.platform.achievements.domain.AchievementRarity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@Schema(description = "Achievement information")
public class AchievementResponse {
    
    @Schema(description = "Unique identifier of the achievement", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;
    
    @Schema(description = "Unique identifier of the game this achievement belongs to", example = "660e8400-e29b-41d4-a716-446655440001")
    private UUID gameId;
    
    @Schema(description = "Name of the achievement", example = "First Victory")
    private String name;
    
    @Schema(description = "Brief description of the achievement", example = "Win your first game")
    private String description;
    
    @Schema(description = "Category of the achievement", example = "PROGRESSION")
    private AchievementCategory category;
    
    @Schema(description = "Rarity of the achievement", example = "COMMON")
    private AchievementRarity rarity;

    public static AchievementResponse fromDomain(Achievement achievement) {
        return AchievementResponse.builder()
                .id(achievement.getId())
                .gameId(achievement.getGameId())
                .name(achievement.getName())
                .description(achievement.getDescription())
                .category(achievement.getCategory())
                .rarity(achievement.getRarity())
                .build();
    }
}

