package com.banditgames.platform.friends.adapter.persistence;

import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import com.banditgames.platform.friends.port.out.SaveFriendshipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FriendshipPersistenceAdapter implements LoadFriendshipPort, SaveFriendshipPort {

    private final FriendshipRepository friendshipRepository;

    @Override
    public Optional<Friendship> findById(UUID friendshipId) {
        return friendshipRepository.findById(friendshipId)
            .map(this::toDomain);
    }

    @Override
    public Optional<Friendship> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId) {
        return friendshipRepository.findByRequesterIdAndAddresseeId(requesterId, addresseeId)
            .map(this::toDomain);
    }

    @Override
    public List<Friendship> findAcceptedFriendshipsByUserId(UUID userId) {
        return friendshipRepository.findByRequesterIdAndStatusOrAddresseeIdAndStatus(
            userId, FriendshipStatus.ACCEPTED, userId, FriendshipStatus.ACCEPTED
        ).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Friendship> findPendingFriendRequests(UUID userId) {
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Friendship> findBlockedFriendRequestsByUserId(UUID userId) {
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.BLOCKED).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Friendship> finedFriendRequestsByUserIdAndStatus(UUID userId, FriendshipStatus status){
        return friendshipRepository.searchByAddresseeIdAndStatus(userId, status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Friendship> findFriendRequestsByUserIdAndStatus(UUID userId, FriendshipStatus status, Pageable pageable) {
        return friendshipRepository.searchByAddresseeIdAndStatus(userId, status, pageable)
                .map(this::toDomain);
    }

    @Override
    public List<Friendship> findRejectedFriendRequestsByUserId(UUID userId) {
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.REJECTED).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }


    @Override
    public boolean existsByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId) {
        return friendshipRepository.existsByRequesterIdAndAddresseeId(requesterId, addresseeId);
    }

    @Override
    public Friendship save(Friendship friendship) {
        FriendshipEntity entity = toEntity(friendship);
        FriendshipEntity saved = friendshipRepository.save(entity);
        return toDomain(saved);
    }

    private Friendship toDomain(FriendshipEntity entity) {
        return Friendship.builder()
            .id(entity.getId())
            .requesterId(entity.getRequesterId())
            .addresseeId(entity.getAddresseeId())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private FriendshipEntity toEntity(Friendship friendship) {
        return FriendshipEntity.builder()
            .id(friendship.getId())
            .requesterId(friendship.getRequesterId())
            .addresseeId(friendship.getAddresseeId())
            .status(friendship.getStatus())
            .createdAt(friendship.getCreatedAt())
            .updatedAt(friendship.getUpdatedAt())
            .build();
    }
}
