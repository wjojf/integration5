package com.banditgames.platform.friends.usecase;

import com.banditgames.platform.acl.adapter.dto.PlayerInfo;
import com.banditgames.platform.acl.port.out.PlayerContextPort;
import com.banditgames.platform.friends.adapter.web.dto.FriendInfoResponse;
import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import com.banditgames.platform.friends.port.in.GetFriendsListByStatusUseCase;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetFriendsService implements GetFriendsListByStatusUseCase {

    private final LoadFriendshipPort loadFriendshipPort;
    private final PlayerContextPort playerContextPort;

    @Override
    public List<Friendship> getFriendsByStatus(UUID userId, FriendshipStatus status) {
        return switch (status) {
            case PENDING -> loadFriendshipPort.findPendingFriendRequests(userId);
            case ACCEPTED -> loadFriendshipPort.findAcceptedFriendshipsByUserId(userId);
            case BLOCKED -> loadFriendshipPort.findBlockedFriendRequestsByUserId(userId);
            case REJECTED -> loadFriendshipPort.findRejectedFriendRequestsByUserId(userId);
            default -> throw new IllegalArgumentException("Invalid friendship status");
        };

    }

    @Override
    public List<Friendship> searchFriendsByStatus(UUID userId, FriendshipStatus status) {
        return loadFriendshipPort.finedFriendRequestsByUserIdAndStatus(userId, status);
    }

    /**
     * Gets friends list with player information enriched (paginated).
     * Returns a paginated list of unique friends (the other user in each friendship, excluding the current user).
     * Removes duplicates so each friend appears only once.
     * Uses repository-level pagination for efficient data fetching.
     *
     * @param userId The user ID to get friends for (current authenticated user)
     * @param status The friendship status to filter by
     * @param pageable Pagination parameters
     * @return Page of enriched friend responses with player information (excluding the current user)
     */
    public Page<FriendInfoResponse> getFriendsWithPlayerInfo(UUID userId, FriendshipStatus status, Pageable pageable) {
        Page<Friendship> friendshipsPage = loadFriendshipPort.findFriendRequestsByUserIdAndStatus(userId, status, pageable);
        List<Friendship> friendships = friendshipsPage.getContent();

        // Collect all unique player IDs from friendships (excluding the current user)
        Set<UUID> otherPlayerIds = friendships.stream()
                .flatMap(friendship -> {
                    UUID requesterId = friendship.getRequesterId();
                    UUID addresseeId = friendship.getAddresseeId();
                    // Return only the other user (not the current user)
                    if (requesterId.equals(userId)) {
                        return List.of(addresseeId).stream();
                    } else {
                        return List.of(requesterId).stream();
                    }
                })
                .collect(Collectors.toSet());

        List<PlayerInfo> playerInfos = playerContextPort.getPlayerInfos(List.copyOf(otherPlayerIds));
        Map<UUID, PlayerInfo> playerInfoMap = playerInfos.stream()
                .collect(Collectors.toMap(PlayerInfo::getPlayerId, Function.identity()));

        // Build enriched responses - map each friendship to the other user's info
        // Group by other user ID to ensure no duplicates (take the first friendship for each user)
        List<FriendInfoResponse> friends = friendships.stream()
                .map(friendship -> toFriendInfoResponse(friendship, userId, playerInfoMap))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(
                        response -> response.getPlayer().getPlayerId(),
                        Function.identity(),
                        (existing, replacement) -> existing // Keep first occurrence if duplicate
                ))
                .values()
                .stream()
                .sorted((a, b) -> {
                    // Sort by createdAt descending (most recent first)
                    // This matches the repository ordering
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        // Return paginated result using the repository page metadata
        // Note: After deduplication, the actual count may be less than the repository count
        // We use the repository's total for accurate pagination metadata
        return new PageImpl<>(friends, pageable, friendshipsPage.getTotalElements());
    }

    /**
     * Converts a Friendship domain object to a friend info response.
     * Returns the other user's information (not the current user).
     */
    private Optional<FriendInfoResponse> toFriendInfoResponse(Friendship friendship, UUID currentUserId, Map<UUID, PlayerInfo> playerInfoMap) {
        UUID otherUserId;

        // Determine which user is the "other" user (not the current user)
        if (friendship.getRequesterId().equals(currentUserId)) {
            otherUserId = friendship.getAddresseeId();
        } else if (friendship.getAddresseeId().equals(currentUserId)) {
            otherUserId = friendship.getRequesterId();
        } else {
            log.error("empty friendship");
            return Optional.empty();
        }

        Optional<PlayerInfo> otherPlayerInfoOpt = Optional.ofNullable(playerInfoMap.get(otherUserId));

        // If player info is missing, create a minimal PlayerInfo (shouldn't happen in normal flow)
        // This handles edge cases where a player might have been deleted but friendship still exists
        PlayerInfo otherPlayerInfo = otherPlayerInfoOpt.orElseGet(() -> PlayerInfo.builder()
                .playerId(otherUserId)
                .build());

        return Optional.of(FriendInfoResponse.builder()
                .friendshipId(friendship.getId())
                .player(otherPlayerInfo)
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .updatedAt(friendship.getUpdatedAt())
                .build());
    }
}

