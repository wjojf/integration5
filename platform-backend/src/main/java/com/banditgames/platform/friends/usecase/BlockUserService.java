package com.banditgames.platform.friends.usecase;

import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import com.banditgames.platform.friends.port.in.BlockUserUseCase;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import com.banditgames.platform.friends.port.out.SaveFriendshipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockUserService implements BlockUserUseCase {
    
    private final LoadFriendshipPort loadFriendshipPort;
    private final SaveFriendshipPort saveFriendshipPort;
    
    @Override
    @Transactional
    public Friendship blockUser(UUID userId, UUID userToBlockId) {
        if (userId.equals(userToBlockId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }
        
        // Check if friendship already exists
        Friendship friendship = loadFriendshipPort.findByRequesterIdAndAddresseeId(userId, userToBlockId)
            .or(() -> loadFriendshipPort.findByRequesterIdAndAddresseeId(userToBlockId, userId))
            .orElse(null);
        
        if (friendship == null) {
            // Create new blocked friendship
            friendship = Friendship.builder()
                .id(UUID.randomUUID())
                .requesterId(userId)
                .addresseeId(userToBlockId)
                .status(FriendshipStatus.BLOCKED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        } else {
            // Update existing friendship to blocked
            friendship.block();
            friendship = Friendship.builder()
                .id(friendship.getId())
                .requesterId(friendship.getRequesterId())
                .addresseeId(friendship.getAddresseeId())
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        }
        
        return saveFriendshipPort.save(friendship);
    }
}

