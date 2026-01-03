package com.banditgames.platform.friends.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Action to perform on a friendship. Different actions are available based on the friendship status and user role.")
public enum FriendshipAction {
    @Schema(description = "Accept the friend request - changes status to ACCEPTED. Only addressee can perform on PENDING requests.")
    ACCEPT,
    
    @Schema(description = "Reject the friend request - changes status to REJECTED. Only addressee can perform on PENDING requests.")
    REJECT,
    
    @Schema(description = "Block the user - changes status to BLOCKED and prevents future friend requests. Can be performed from PENDING or ACCEPTED status.")
    BLOCK,
    
    @Schema(description = "Cancel a pending friend request - changes status to REJECTED. Only requester can perform on PENDING requests.")
    CANCEL,
    
    @Schema(description = "Remove an accepted friend - changes status to REJECTED. Either user can perform on ACCEPTED friendships.")
    REMOVE,
    
    @Schema(description = "Unblock a blocked user - changes status to REJECTED. Only the user who blocked can perform on BLOCKED friendships.")
    UNBLOCK
}
