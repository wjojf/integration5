package com.banditgames.platform.achievements.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievementEntity, UUID> {
    
    List<UserAchievementEntity> findByUserId(String userId);
    
    List<UserAchievementEntity> findByUserIdAndAchievementGameId(String userId, UUID gameId);
    
    boolean existsByUserIdAndAchievementId(String userId, UUID achievementId);
    
    java.util.Optional<UserAchievementEntity> findByUserIdAndAchievementId(String userId, UUID achievementId);
}
