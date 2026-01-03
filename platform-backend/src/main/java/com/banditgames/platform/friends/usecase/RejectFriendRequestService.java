package com.banditgames.platform.friends.usecase;

import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.exception.FriendshipNotFoundException;
import com.banditgames.platform.friends.domain.exception.FriendshipOperationException;
import com.banditgames.platform.friends.port.in.RejectFriendRequestUseCase;
import com.banditgames.platform.friends.port.out.LoadFriendshipPort;
import com.banditgames.platform.friends.port.out.SaveFriendshipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RejectFriendRequestService implements RejectFriendRequestUseCase {
    
    private final LoadFriendshipPort loadFriendshipPort;
    private final SaveFriendshipPort saveFriendshipPort;
    
    @Override
    @Transactional
    public Friendship rejectFriendRequest(UUID friendshipId, UUID userId) {
        Friendship friendship = loadFriendshipPort.findById(friendshipId)
            .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found: " + friendshipId));
        
        if (!friendship.getAddresseeId().equals(userId)) {
            throw new FriendshipOperationException("Only the addressee can reject a friend request");
        }
        
        try {
            friendship.reject();
            friendship = Friendship.builder()
                .id(friendship.getId())
                .requesterId(friendship.getRequesterId())
                .addresseeId(friendship.getAddresseeId())
                .status(friendship.getStatus())
                .createdAt(friendship.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
        } catch (IllegalStateException e) {
            throw new FriendshipOperationException(e.getMessage(), e);
        }
        
        return saveFriendshipPort.save(friendship);
    }
}

