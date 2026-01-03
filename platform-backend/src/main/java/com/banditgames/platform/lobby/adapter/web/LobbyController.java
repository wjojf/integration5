package com.banditgames.platform.lobby.adapter.web;

import com.banditgames.platform.lobby.adapter.web.dto.CreateLobbyRequest;
import com.banditgames.platform.lobby.adapter.web.dto.InviteToLobbyRequest;
import com.banditgames.platform.lobby.adapter.web.dto.LobbyResponse;
import com.banditgames.platform.lobby.adapter.web.dto.StartLobbyRequest;
import com.banditgames.platform.lobby.adapter.web.dto.UpdateLobbyRequest;
import com.banditgames.platform.lobby.domain.Lobby;
import com.banditgames.platform.lobby.port.in.*;
import com.banditgames.platform.lobby.port.in.UpdateLobbyUseCase;
import com.banditgames.platform.acl.port.out.PlayerContextPort;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/lobbies")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Lobbies", description = "API for managing game lobbies")
@SecurityRequirement(name = "bearerAuth")
public class LobbyController {

    private final CreateLobbyUseCase createLobbyUseCase;
    private final GetLobbyUseCase getLobbyUseCase;
    private final JoinLobbyUseCase joinLobbyUseCase;
    private final LeaveLobbyUseCase leaveLobbyUseCase;
    private final StartLobbyUseCase startLobbyUseCase;
    private final InviteToLobbyUseCase inviteToLobbyUseCase;
    private final UpdateLobbyUseCase updateLobbyUseCase;
    private final SearchLobbyUseCase searchLobbyUseCase;
    private final PlayerContextPort playerContextPort;
    private final com.banditgames.platform.lobby.service.ExternalGameInstanceService externalGameInstanceService;

    @Operation(
        summary = "Create a new lobby",
        description = "Creates a new game lobby. The authenticated user becomes the host and automatically joins the lobby."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Lobby created successfully",
            content = @Content(schema = @Schema(implementation = LobbyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<LobbyResponse> createLobby(@Valid @RequestBody CreateLobbyRequest request) {
        UUID userId = AuthUtils.getCurrentUserId();
        var lobby = createLobbyUseCase.createLobby(
                userId,
                request.getName(),
                request.getDescription(),
                request.getMaxPlayers(),
                request.isPrivate()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LobbyResponse.fromDomain(lobby));
    }

    @Operation(
        summary = "Join a lobby",
        description = "Join an existing lobby. For private lobbies, the user must have been invited."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully joined lobby",
            content = @Content(schema = @Schema(implementation = LobbyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Cannot join lobby (full, already started, or not invited)"),
        @ApiResponse(responseCode = "404", description = "Lobby not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{lobbyId}/join")
    public ResponseEntity<LobbyResponse> joinLobby(
        @Parameter(description = "Lobby ID", required = true) @PathVariable UUID lobbyId) {
        UUID userId = AuthUtils.getCurrentUserId();
        var lobby = joinLobbyUseCase.joinLobby(lobbyId, userId);
        return ResponseEntity.ok(LobbyResponse.fromDomain(lobby));
    }

    @Operation(
        summary = "Leave a lobby",
        description = "Leave a lobby. If the host leaves, the lobby is cancelled."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully left lobby",
            content = @Content(schema = @Schema(implementation = LobbyResponse.class))),
        @ApiResponse(responseCode = "404", description = "Lobby not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{lobbyId}/leave")
    public ResponseEntity<LobbyResponse> leaveLobby(
        @Parameter(description = "Lobby ID", required = true) @PathVariable UUID lobbyId) {
        UUID userId = AuthUtils.getCurrentUserId();
        var lobby = leaveLobbyUseCase.leaveLobby(lobbyId, userId);
        return ResponseEntity.ok(LobbyResponse.fromDomain(lobby));
    }

    @Operation(
        summary = "Start a lobby",
        description = "Start a lobby when a game is selected. Only the host can start the lobby."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lobby started successfully",
            content = @Content(schema = @Schema(implementation = LobbyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Cannot start lobby (no game selected, no players, etc.)"),
        @ApiResponse(responseCode = "403", description = "Only the host can start the lobby"),
        @ApiResponse(responseCode = "404", description = "Lobby not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{lobbyId}/start")
    public ResponseEntity<LobbyResponse> startLobby(
        @Parameter(description = "Lobby ID", required = true) @PathVariable UUID lobbyId,
        @Valid @RequestBody StartLobbyRequest request) {
        UUID userId = AuthUtils.getCurrentUserId();
        var lobby = startLobbyUseCase.startLobby(lobbyId, userId, request.getGameId());
        return ResponseEntity.ok(LobbyResponse.fromDomain(lobby));
    }

    @Operation(
        summary = "Invite a friend to a private lobby",
        description = "Invite a friend to join a private lobby. Only the host can invite players."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Friend invited successfully",
            content = @Content(schema = @Schema(implementation = LobbyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Cannot invite (lobby is public, already invited, etc.)"),
        @ApiResponse(responseCode = "403", description = "Only the host can invite players"),
        @ApiResponse(responseCode = "404", description = "Lobby not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{lobbyId}/invite")
    public ResponseEntity<LobbyResponse> inviteToLobby(
            @Parameter(description = "Lobby ID", required = true) @PathVariable UUID lobbyId,
            @Valid @RequestBody InviteToLobbyRequest request
    ) {
        UUID userId = AuthUtils.getCurrentUserId();
        var lobby = inviteToLobbyUseCase.inviteToLobby(lobbyId, userId, request.getInvitedPlayerId());
        return ResponseEntity.ok(LobbyResponse.fromDomain(lobby));
    }

    @Operation(
        summary = "Update lobby name and description",
        description = "Update the name and description of a lobby. Only the host can update the lobby, and only when it is in WAITING status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lobby updated successfully",
            content = @Content(schema = @Schema(implementation = LobbyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Cannot update lobby (not in WAITING status)"),
        @ApiResponse(responseCode = "403", description = "Only the host can update the lobby"),
        @ApiResponse(responseCode = "404", description = "Lobby not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/{lobbyId}")
    public ResponseEntity<LobbyResponse> updateLobby(
            @Parameter(description = "Lobby ID", required = true) @PathVariable UUID lobbyId,
            @Valid @RequestBody UpdateLobbyRequest request
    ) {
        UUID userId = AuthUtils.getCurrentUserId();
        var lobby = updateLobbyUseCase.updateLobby(lobbyId, userId, request.getName(), request.getDescription());
        return ResponseEntity.ok(LobbyResponse.fromDomain(lobby));
    }

    @Operation(
        summary = "Get lobby details",
        description = "Retrieve details of a specific lobby by ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lobby found",
            content = @Content(schema = @Schema(implementation = LobbyResponse.class))),
        @ApiResponse(responseCode = "404", description = "Lobby not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{lobbyId}")
    public ResponseEntity<LobbyResponse> getLobby(
        @Parameter(description = "Lobby ID", required = true) @PathVariable UUID lobbyId) {
        return getLobbyUseCase.getLobby(lobbyId)
                .map(LobbyResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Get current user's lobby",
        description = "Retrieve the current authenticated user's active lobby. Returns the most recent active lobby the user is a member of."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lobby found",
            content = @Content(schema = @Schema(implementation = LobbyResponse.class))),
        @ApiResponse(responseCode = "404", description = "User is not in any active lobby"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/current")
    public ResponseEntity<LobbyResponse> getCurrentUserLobby() {
        UUID userId = AuthUtils.getCurrentUserId();
        return getLobbyUseCase.getLobbyByUserId(userId)
                .map(LobbyResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Search for lobbies",
        description = "Search and filter public waiting lobbies based on game ID and/or host username. " +
                "All query parameters are optional and can be combined. This endpoint only returns public lobbies " +
                "that are in WAITING status (not started yet). The controller coordinates between the player and lobby " +
                "modules following modulith conventions: first resolves username to player IDs if provided, then " +
                "searches lobbies by game ID and host IDs."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lobbies found successfully. Returns a paginated response with lobby details matching the search criteria. " +
                    "If no lobbies match the criteria, an empty page is returned.",
            content = @Content(
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    name = "Search results",
                    value = """
                        {
                          "content": [
                            {
                              "id": "550e8400-e29b-41d4-a716-446655440000",
                              "gameId": "660e8400-e29b-41d4-a716-446655440001",
                              "hostId": "770e8400-e29b-41d4-a716-446655440002",
                              "playerIds": [
                                "770e8400-e29b-41d4-a716-446655440002"
                              ],
                              "status": "WAITING",
                              "maxPlayers": 4,
                              "visibility": "PUBLIC",
                              "invitedPlayerIds": [],
                              "createdAt": "2023-12-06T15:30:00",
                              "startedAt": null
                            },
                            {
                              "id": "660e8400-e29b-41d4-a716-446655440003",
                              "gameId": "660e8400-e29b-41d4-a716-446655440001",
                              "hostId": "880e8400-e29b-41d4-a716-446655440004",
                              "playerIds": [
                                "880e8400-e29b-41d4-a716-446655440004",
                                "990e8400-e29b-41d4-a716-446655440005"
                              ],
                              "status": "WAITING",
                              "maxPlayers": 6,
                              "visibility": "PUBLIC",
                              "invitedPlayerIds": [],
                              "createdAt": "2023-12-06T15:45:00",
                              "startedAt": null
                            }
                          ],
                          "pageable": {
                            "pageNumber": 0,
                            "pageSize": 20,
                            "sort": {
                              "sorted": false,
                              "unsorted": true,
                              "empty": true
                            },
                            "offset": 0,
                            "paged": true,
                            "unpaged": false
                          },
                          "totalElements": 2,
                          "totalPages": 1,
                          "last": true,
                          "first": true,
                          "size": 20,
                          "number": 0,
                          "numberOfElements": 2,
                          "empty": false
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid query parameters",
            content = @Content(
                examples = @ExampleObject(
                    name = "Invalid UUID format",
                    value = """
                        {
                          "message": "Invalid UUID format for gameId parameter"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/search")
    public ResponseEntity<Page<LobbyResponse>> searchLobbies(
            @Parameter(
                description = "Filter by game ID. Returns only lobbies for this specific game.",
                required = false,
                example = "660e8400-e29b-41d4-a716-446655440001",
                schema = @Schema(format = "uuid")
            )
            @RequestParam(required = false) UUID gameId,
            @Parameter(
                description = "Filter by host username. Searches for players whose username contains this value " +
                        "(case-insensitive, partial match) and returns lobbies hosted by those players. " +
                        "Example: 'gamer' will match lobbies hosted by 'GamerPro', 'ProGamer', etc.",
                required = false,
                example = "gamer"
            )
            @RequestParam(required = false) String username,
            @Parameter(
                description = "Pagination parameters (page, size, sort). Default page size is 20.",
                required = false
            )
            @PageableDefault(size = 20) Pageable pageable) {

        List<UUID> hostIds = null;

        if (username != null && !username.trim().isEmpty()) {
            // Use ACL to access Player context - ensures loose coupling
            hostIds = playerContextPort.findPlayerIdsByUsername(username);

            if (hostIds.isEmpty()) {
                return ResponseEntity.ok(Page.empty(pageable));
            }
        }

        Page<Lobby> lobbies = searchLobbyUseCase.searchLobbies(gameId, hostIds, pageable);
        Page<LobbyResponse> response = lobbies.map(LobbyResponse::fromDomain);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get external game instance ID for a lobby",
        description = "Retrieves the external game instance ID associated with a lobby. " +
                "Returns null if the lobby is not for an external game or if the game hasn't been created yet."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "External game instance ID retrieved (may be null)"),
        @ApiResponse(responseCode = "404", description = "Lobby not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{lobbyId}/external-game-instance")
    public ResponseEntity<Map<String, Object>> getExternalGameInstance(
        @Parameter(description = "Lobby ID", required = true) @PathVariable UUID lobbyId) {
        
        // Verify lobby exists
        Lobby lobby = getLobbyUseCase.getLobby(lobbyId)
                .orElseThrow(() -> new com.banditgames.platform.lobby.domain.exception.LobbyNotFoundException("Lobby not found: " + lobbyId));
        
        // Get external game instance if it exists
        UUID externalGameInstanceId = externalGameInstanceService.getExternalGameInstanceId(lobbyId);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("lobbyId", lobbyId);
        response.put("gameId", lobby.getGameId());
        response.put("externalGameInstanceId", externalGameInstanceId);
        response.put("hasExternalGameInstance", externalGameInstanceId != null);
        
        return ResponseEntity.ok(response);
    }
}

