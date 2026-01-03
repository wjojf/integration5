package com.banditgames.platform.achievements.usecase;

import com.banditgames.platform.achievements.domain.UserAchievement;
import com.banditgames.platform.achievements.port.in.SavePlayerAcquiredNewAchievementUseCase;
import com.banditgames.platform.achievements.port.out.SaveUserAchievementPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for saving player achievements.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SavePlayerAcquiredNewAchievementService implements SavePlayerAcquiredNewAchievementUseCase {

    private final SaveUserAchievementPort saveUserAchievementPort;

    @Override
    public UserAchievement saveNewAchievement(SavePlayerAcquiredNewAchievement record) {
        log.info("Saving achievement for player: {}, achievement: {}", 
                record.playerId(), record.achievementId());
        
        UserAchievement userAchievement = UserAchievement.builder()
                .userId(record.playerId().toString())
                .achievementId(record.achievementId())
                .build();
        
        return saveUserAchievementPort.save(userAchievement);
    }

    @Override
    public UserAchievement saveNewThirdPartyAchievement(SavePlayerAcquiredNewThirdPartyAchievement record) {
        log.info("Saving third-party achievement for player: {}, code: {}", 
                record.playerId(), record.achievementCode());
        
        // For third-party achievements, we need to find the achievement by code
        // This would require a repository method to find by code
        // For now, we'll log and return null - this should be implemented when third-party achievements are used
        log.warn("Third-party achievement saving not fully implemented - player: {}, code: {}", 
                record.playerId(), record.achievementCode());
        
        // TODO: Implement third-party achievement lookup by code
        return null;
    }
    
    @Override
    public boolean hasAchievement(UUID playerId, UUID achievementId) {
        return saveUserAchievementPort.existsByPlayerIdAndAchievementId(playerId, achievementId);
    }
}
