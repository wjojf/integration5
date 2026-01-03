package com.banditgames.platform.friends.adapter.web.dto;

import com.banditgames.platform.friends.domain.FriendshipAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to modify a friendship status")
public class ModifyFriendshipRequest {

    @NotNull(message = "Action is required")
    @Schema(
            description = "Action to take on the friendship. Available actions depend on friendship status: " +
                    "PENDING: ACCEPT (addressee), REJECT (addressee), BLOCK (addressee), CANCEL (requester); " +
                    "ACCEPTED: REMOVE (either user), BLOCK (either user); " +
                    "BLOCKED: UNBLOCK (user who blocked). This field is required and cannot be null.",
            example = "ACCEPT",
            allowableValues = {"ACCEPT", "REJECT", "BLOCK", "CANCEL", "REMOVE", "UNBLOCK"}
    )
    FriendshipAction action;
}
