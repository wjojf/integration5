package com.banditgames.platform.friends.adapter.web;

import com.banditgames.platform.friends.adapter.web.dto.FriendInfoResponse;
import com.banditgames.platform.friends.adapter.web.dto.FriendshipResponse;
import com.banditgames.platform.friends.adapter.web.dto.ModifyFriendshipRequest;
import com.banditgames.platform.friends.adapter.web.dto.SendFriendRequestRequest;
import com.banditgames.platform.friends.domain.Friendship;
import com.banditgames.platform.friends.domain.FriendshipStatus;
import com.banditgames.platform.friends.port.in.PatchFriendRequestUseCase;
import com.banditgames.platform.friends.port.in.SendFriendRequestUseCase;
import com.banditgames.platform.friends.usecase.GetFriendsService;
import com.banditgames.platform.shared.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Friends", description = "API for managing friendships and friend requests")
@SecurityRequirement(name = "bearerAuth")
public class FriendsController {

    private final SendFriendRequestUseCase sendFriendRequestUseCase;
    private final PatchFriendRequestUseCase patchFriendRequestUseCase;
    private final GetFriendsService getFriendsService;

    @Operation(
            summary = "Send a friend request",
            description = "Send a friend request to another user. The authenticated user becomes the requester, and the specified user becomes the addressee. " +
                    "Validation rules: addresseeId must be a valid UUID and cannot be null or the same as the authenticated user's ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Friend request sent successfully",
                    content = @Content(
                            schema = @Schema(implementation = FriendshipResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful friend request",
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "requesterId": "123e4567-e89b-12d3-a456-426614174001",
                                              "addresseeId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                              "status": "PENDING",
                                              "createdAt": "2023-12-06T10:15:30",
                                              "updatedAt": "2023-12-06T10:15:30"
                                            }
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "400", description = "Invalid request - cannot send friend request to yourself, or a request already exists between the users",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "message": "Cannot send friend request to yourself"
                                            }
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "User not found - the addressee user does not exist")
    })
    @PostMapping("/requests")
    public ResponseEntity<FriendshipResponse> sendFriendRequest(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Friend request details containing the addressee user ID. Validation: addresseeId is required and must be a valid UUID.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SendFriendRequestRequest.class),
                            examples = @ExampleObject(
                                    name = "Send friend request",
                                    value = """
                                            {
                                              "addresseeId": "987fcdeb-51a2-43f1-b789-123456789abc"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody SendFriendRequestRequest request) {
        UUID userId = AuthUtils.getCurrentUserId();
        Friendship friendship = sendFriendRequestUseCase.sendFriendRequest(userId, request.getAddresseeId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FriendshipResponse.fromDomain(friendship));
    }

    @Operation(
            summary = "Get friends list by status (paginated)",
            description = "Retrieve a paginated list of friends for the authenticated user filtered by their status (PENDING, ACCEPTED, REJECTED, or BLOCKED). " +
                    "Returns enriched friend objects with full player information. The current authenticated user is excluded from the results, " +
                    "and each friend appears only once (no duplicates). " +
                    "Default page size is 20. Use page and size query parameters for pagination. " +
                    "Validation rules: status parameter is required and must be one of the valid FriendshipStatus enum values."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Friends list retrieved successfully. Returns a paginated response with friend objects containing player information.",
                    content = @Content(
                            schema = @Schema(implementation = Page.class),
                            examples = @ExampleObject(
                                    name = "Paginated list of pending friend requests with player info",
                                    value = """
                                            {
                                              "content": [
                                                {
                                                  "friendshipId": "550e8400-e29b-41d4-a716-446655440000",
                                                  "player": {
                                                    "playerId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                                    "username": "PlayerTwo",
                                                    "bio": "Strategy games lover",
                                                    "rank": "SILVER",
                                                    "exp": 1500,
                                                    "gamePreferences": []
                                                  },
                                                  "status": "PENDING",
                                                  "createdAt": "2023-12-06T10:15:30",
                                                  "updatedAt": "2023-12-06T10:15:30"
                                                }
                                              ],
                                              "pageable": {
                                                "pageNumber": 0,
                                                "pageSize": 20
                                              },
                                              "totalElements": 1,
                                              "totalPages": 1
                                            }
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "400", description = "Invalid status parameter - must be one of: PENDING, ACCEPTED, REJECTED, BLOCKED")
    })
    @GetMapping
    public ResponseEntity<Page<FriendInfoResponse>> getFriendsListByStatus(
            @Parameter(
                    description = "Filter friendships by their status. Validation: Must be one of PENDING (friend requests awaiting response), ACCEPTED (active friendships), REJECTED (declined requests), BLOCKED (blocked users). This parameter is required.",
                    example = "PENDING",
                    schema = @Schema(allowableValues = {"PENDING", "ACCEPTED", "REJECTED", "BLOCKED"})
            )
            @RequestParam(required = false) FriendshipStatus status,
            @Parameter(
                    description = "Pagination parameters. Default page size is 20. Use 'page' for page number (0-based) and 'size' for page size.",
                    example = "0"
            )
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        UUID userId = AuthUtils.getCurrentUserId();
        Page<FriendInfoResponse> friends = getFriendsService.getFriendsWithPlayerInfo(userId, status, pageable);
        return ResponseEntity.ok(friends);
    }

    @Operation(
            summary = "Modify a friendship",
            description = "Modify a friendship based on its current status. Available actions: " +
                    "PENDING: ACCEPT (addressee only), REJECT (addressee only), BLOCK (addressee only), CANCEL (requester only); " +
                    "ACCEPTED: REMOVE (either user), BLOCK (either user); " +
                    "BLOCKED: UNBLOCK (user who blocked only). " +
                    "Validation rules: friendshipId must be a valid UUID, action is required and must match the friendship status and user role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Friendship modified successfully with the specified action",
                    content = @Content(
                            schema = @Schema(implementation = FriendshipResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Accepted friend request",
                                            value = """
                                                    {
                                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                                      "requesterId": "123e4567-e89b-12d3-a456-426614174001",
                                                      "addresseeId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                                      "status": "ACCEPTED",
                                                      "createdAt": "2023-12-06T10:15:30",
                                                      "updatedAt": "2023-12-06T10:20:45"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Cancelled friend request",
                                            value = """
                                                    {
                                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                                      "requesterId": "123e4567-e89b-12d3-a456-426614174001",
                                                      "addresseeId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                                      "status": "REJECTED",
                                                      "createdAt": "2023-12-06T10:15:30",
                                                      "updatedAt": "2023-12-06T10:20:45"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Removed friend",
                                            value = """
                                                    {
                                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                                      "requesterId": "123e4567-e89b-12d3-a456-426614174001",
                                                      "addresseeId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                                      "status": "REJECTED",
                                                      "createdAt": "2023-12-06T10:15:30",
                                                      "updatedAt": "2023-12-06T10:20:45"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Unblocked user",
                                            value = """
                                                    {
                                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                                      "requesterId": "123e4567-e89b-12d3-a456-426614174001",
                                                      "addresseeId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                                      "status": "REJECTED",
                                                      "createdAt": "2023-12-06T10:15:30",
                                                      "updatedAt": "2023-12-06T10:20:45"
                                                    }
                                                    """
                                    )
                            }
                    )),
            @ApiResponse(responseCode = "400", description = "Bad request - action not allowed for current friendship status or user role",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Invalid modification attempt",
                                    value = """
                                            {
                                              "message": "Only the addressee can modify the friend request"
                                            }
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Friendship not found - no friendship exists with the specified ID",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Friendship not found",
                                    value = """
                                            {
                                              "message": "Friendship not found with ID: 550e8400-e29b-41d4-a716-446655440000"
                                            }
                                            """
                            )
                    ))
    })
    @PatchMapping("/requests/{friendshipId}")
    public ResponseEntity<FriendshipResponse> patchFriendRequest(
            @Parameter(
                    description = "Unique identifier of the friendship to modify. Validation: Must be a valid UUID format.",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    schema = @Schema(format = "uuid")
            )
            @PathVariable UUID friendshipId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Action to perform on the friendship. Available actions: ACCEPT, REJECT, BLOCK, CANCEL, REMOVE, UNBLOCK. " +
                            "Action availability depends on friendship status and user role. Validation: action field is required.",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ModifyFriendshipRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Accept friend request (PENDING, addressee)",
                                            value = """
                                                    {
                                                      "action": "ACCEPT"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Cancel friend request (PENDING, requester)",
                                            value = """
                                                    {
                                                      "action": "CANCEL"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Remove friend (ACCEPTED, either user)",
                                            value = """
                                                    {
                                                      "action": "REMOVE"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Unblock user (BLOCKED, user who blocked)",
                                            value = """
                                                    {
                                                      "action": "UNBLOCK"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @Valid @RequestBody ModifyFriendshipRequest request) {
        UUID userId = AuthUtils.getCurrentUserId();
        Friendship friendship = patchFriendRequestUseCase.patchFriendRequest(friendshipId, userId, request.getAction());
        return ResponseEntity.ok(FriendshipResponse.fromDomain(friendship));
    }
}

