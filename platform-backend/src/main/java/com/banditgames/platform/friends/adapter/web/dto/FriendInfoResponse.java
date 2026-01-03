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
@Schema(description = "Friend information response containing player details and friendship metadata")
public class FriendInfoResponse {
    
    @Schema(
            description = "Unique identifier of the friendship record. UUID format.",
            example = "123e4567-e89b-12d3-a456-426614174000",
            format = "uuid"
    )
    private UUID friendshipId;
    
    @Schema(
            description = "Player information for the friend (the other user in the friendship, not the current authenticated user)"
    )
    private PlayerInfo player;
    
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

