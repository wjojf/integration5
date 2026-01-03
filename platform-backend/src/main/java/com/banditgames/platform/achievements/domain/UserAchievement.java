package com.banditgames.platform.achievements.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievement {
    private UUID id;
    private String userId;
    private UUID achievementId;
    private LocalDateTime unlockedAt;
}

