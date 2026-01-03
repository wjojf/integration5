package com.banditgames.platform.friends.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send a friend request")
public class SendFriendRequestRequest {
    
    @NotNull(message = "Addressee ID is required")
    @Schema(
            description = "UUID of the user to send friend request to. Must be a valid UUID format. Cannot be the same as the authenticated user's ID.",
            example = "123e4567-e89b-12d3-a456-426614174000",
            format = "uuid"
    )
    private UUID addresseeId;
}

