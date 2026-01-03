package com.banditgames.platform.player.adapter.persistence;

import com.banditgames.platform.player.domain.Rank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    Page<PlayerEntity> findAllByUsernameOrRank(String username, Rank rank, Pageable pageable);

    @Query("""
    SELECT p
    FROM PlayerEntity p
    WHERE p.playerId <> :userId
      AND (:username IS NULL OR :username = ''
           OR LOWER(p.username) LIKE LOWER(CONCAT('%', :username, '%')))
      AND (:rank IS NULL OR p.rank = :rank)
""")
    Page<PlayerEntity> searchPlayersForUser(UUID userId, String username, Rank rank, Pageable page);

    @Query("""
        SELECT DISTINCT p
        FROM PlayerEntity p, FriendshipEntity f
        WHERE f.status = 'ACCEPTED'
          AND (
            (f.requesterId = :playerId AND p.playerId = f.addresseeId)
            OR
            (f.addresseeId = :playerId AND p.playerId = f.requesterId)
          )
""")
    List<PlayerEntity> findPlayerFriends(UUID playerId);

}

