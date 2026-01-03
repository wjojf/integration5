package com.banditgames.platform.friends.adapter.web.dto;

import com.banditgames.platform.acl.adapter.dto.PlayerInfo;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Friendship relationship response with player information")
public class FriendshipWithPlayerInfoResponse {
    
    @Schema(
            description = "Unique identifier of the friendship record. UUID format.",
            example = "123e4567-e89b-12d3-a456-426614174000",
            format = "uuid"
    )
    private UUID friendshipId;
    
    @Schema(
            description = "UUID of the user who initiated the friend request. This is the sender of the request.",
            example = "123e4567-e89b-12d3-a456-426614174001",
            format = "uuid"
    )
    private UUID requesterId;
    
    @Schema(
            description = "Player information for the requester"
    )
    private PlayerInfo requester;
    
    @Schema(
            description = "UUID of the user who received the friend request. This is the recipient who can accept, reject, or block.",
            example = "123e4567-e89b-12d3-a456-426614174002",
            format = "uuid"
    )
    private UUID addresseeId;
    
    @Schema(
            description = "Player information for the addressee"
    )
    private PlayerInfo addressee;
    
    @Schema(
            description = "Current status of the friendship. PENDING: awaiting response, ACCEPTED: active friendship, REJECTED: declined request, BLOCKED: user has been blocked",
            example = "PENDING",
            allowableValues = {"PENDING", "ACCEPTED", "REJECTED", "BLOCKED"}
    )
    private FriendshipStatus status;
    
    @Schema(
            description = "ISO 8601 timestamp when the friendship request was created",
            example = "2023-12-06T10:15:30",
            format = "date-time"
    )
    private LocalDateTime createdAt;
    
    @Schema(
            description = "ISO 8601 timestamp when the friendship status was last updated",
            example = "2023-12-06T10:20:30",
            format = "date-time"
    )
    private LocalDateTime updatedAt;
}

