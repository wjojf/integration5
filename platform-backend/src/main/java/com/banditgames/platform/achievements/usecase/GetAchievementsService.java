package com.banditgames.platform.achievements.usecase;

import com.banditgames.platform.achievements.adapter.persistence.AchievementMapper;
import com.banditgames.platform.achievements.adapter.persistence.AchievementRepository;
import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.port.in.GetAchievementsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAchievementsService implements GetAchievementsUseCase {

    private final AchievementRepository achievementRepository;
    private final AchievementMapper achievementMapper;

    @Override
    public List<Achievement> getAllAchievements(UUID gameId) {
        if (gameId != null) {
            return achievementRepository.findByGameId(gameId).stream()
                    .map(achievementMapper::toDomain)
                    .collect(Collectors.toList());
        }
        return achievementRepository.findAll().stream()
                .map(achievementMapper::toDomain)
                .collect(Collectors.toList());
    }
}

