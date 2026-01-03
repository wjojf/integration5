package com.banditgames.platform.friends.adapter.persistence;

import com.banditgames.platform.friends.domain.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<FriendshipEntity, UUID> {

    List<FriendshipEntity> findByRequesterIdAndStatusOrAddresseeIdAndStatus(
        UUID requesterId, FriendshipStatus status1,
        UUID addresseeId, FriendshipStatus status2
    );

    List<FriendshipEntity> findByAddresseeIdAndStatus(UUID addresseeId, FriendshipStatus status);

    @Query("""
        SELECT f
        FROM FriendshipEntity f
        WHERE (:status IS NULL OR f.status = :status) AND (:userId = f.requesterId OR :userId = f.addresseeId)
        ORDER BY f.createdAt DESC
    """)
    List<FriendshipEntity> searchByAddresseeIdAndStatus(UUID userId, FriendshipStatus status);

    @Query("""
        SELECT f
        FROM FriendshipEntity f
        WHERE (:status IS NULL OR f.status = :status) AND (:userId = f.requesterId OR :userId = f.addresseeId)
        ORDER BY f.createdAt DESC
    """)
    Page<FriendshipEntity> searchByAddresseeIdAndStatus(UUID userId, FriendshipStatus status, Pageable pageable);

    Optional<FriendshipEntity> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);

    boolean existsByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);
}
