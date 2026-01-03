package com.banditgames.platform.achievements.adapter.persistence;

import com.banditgames.platform.achievements.domain.UserAchievement;
import com.banditgames.platform.achievements.port.out.SaveUserAchievementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Adapter for saving user achievements.
 */
@Component
@RequiredArgsConstructor
public class UserAchievementPersistenceAdapter implements SaveUserAchievementPort {
    
    private final UserAchievementRepository repository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementMapper mapper;
    
    @Override
    public UserAchievement save(UserAchievement userAchievement) {
        // Load achievement entity
        AchievementEntity achievementEntity = achievementRepository.findById(userAchievement.getAchievementId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Achievement not found: " + userAchievement.getAchievementId()));
        
        // Check if already exists
        if (repository.existsByUserIdAndAchievementId(
                userAchievement.getUserId(), 
                userAchievement.getAchievementId())) {
            // Return existing achievement
            return repository.findByUserIdAndAchievementId(
                    userAchievement.getUserId(), 
                    userAchievement.getAchievementId())
                    .map(mapper::toDomain)
                    .orElse(userAchievement);
        }
        
        // Set unlocked time if not set
        UserAchievement achievementToSave = userAchievement;
        if (userAchievement.getUnlockedAt() == null) {
            achievementToSave = UserAchievement.builder()
                    .id(userAchievement.getId())
                    .userId(userAchievement.getUserId())
                    .achievementId(userAchievement.getAchievementId())
                    .unlockedAt(LocalDateTime.now())
                    .build();
        }
        
        // Convert to entity and save
        UserAchievementEntity entity = mapper.toEntity(achievementToSave, achievementEntity);
        UserAchievementEntity saved = repository.save(entity);
        
        return mapper.toDomain(saved);
    }
    
    @Override
    public boolean existsByPlayerIdAndAchievementId(UUID playerId, UUID achievementId) {
        return repository.existsByUserIdAndAchievementId(
                playerId.toString(), 
                achievementId);
    }
}

