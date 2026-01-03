package com.banditgames.platform.friends.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of a friendship relationship")
public enum FriendshipStatus {
    @Schema(description = "Friend request has been sent and is awaiting response from the addressee")
    PENDING,
    
    @Schema(description = "Friend request has been accepted - users are now friends")
    ACCEPTED,
    
    @Schema(description = "Friend request has been rejected by the addressee")
    REJECTED,
    
    @Schema(description = "User has been blocked by the addressee")
    BLOCKED
}

