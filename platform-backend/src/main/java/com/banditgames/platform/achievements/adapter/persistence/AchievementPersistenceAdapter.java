package com.banditgames.platform.achievements.adapter.persistence;

import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.port.out.LoadAchievementsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter for loading achievements from persistence.
 */
@Component
@RequiredArgsConstructor
public class AchievementPersistenceAdapter implements LoadAchievementsPort {
    
    private final AchievementRepository achievementRepository;
    private final AchievementMapper achievementMapper;
    
    @Override
    public List<Achievement> findByGameId(UUID gameId) {
        return achievementRepository.findByGameId(gameId).stream()
                .map(achievementMapper::toDomain)
                .collect(Collectors.toList());
    }
}

