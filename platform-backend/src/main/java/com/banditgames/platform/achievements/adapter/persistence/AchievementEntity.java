package com.banditgames.platform.achievements.adapter.persistence;

import com.banditgames.platform.achievements.domain.AchievementCategory;
import com.banditgames.platform.achievements.domain.AchievementCriteria;
import com.banditgames.platform.achievements.domain.AchievementRarity;
import com.banditgames.platform.achievements.domain.TriggeringEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "achievements")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID gameId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String triggerConditionString;

    @Column(nullable = false)
    @Builder.Default
    private Boolean thirdPartyAchievement = false;

    @Column
    private String code;

    @Enumerated(EnumType.STRING)
    private AchievementCriteria criteria;

    @Enumerated(EnumType.STRING)
    private TriggeringEventType triggeringEventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementCategory achievementCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementRarity achievementRarity;
}

