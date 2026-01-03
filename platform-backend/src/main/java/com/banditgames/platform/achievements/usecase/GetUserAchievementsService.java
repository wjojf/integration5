package com.banditgames.platform.achievements.usecase;

import com.banditgames.platform.achievements.adapter.persistence.UserAchievementMapper;
import com.banditgames.platform.achievements.adapter.persistence.UserAchievementRepository;
import com.banditgames.platform.achievements.domain.UserAchievement;
import com.banditgames.platform.achievements.port.in.GetUserAchievementsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetUserAchievementsService implements GetUserAchievementsUseCase {

    private final UserAchievementRepository userAchievementRepository;
    private final UserAchievementMapper userAchievementMapper;

    @Override
    public List<UserAchievement> getUserAchievements(UUID userId, UUID gameId) {
        if (gameId != null) {
            return userAchievementRepository.findByUserIdAndAchievementGameId(userId.toString(), gameId).stream()
                    .map(userAchievementMapper::toDomain)
                    .collect(Collectors.toList());
        }
        return userAchievementRepository.findByUserId(userId.toString()).stream()
                .map(userAchievementMapper::toDomain)
                .collect(Collectors.toList());
    }
}

