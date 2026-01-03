package com.banditgames.platform.player.adapter.web;

import com.banditgames.platform.player.adapter.web.dto.PlayerResponse;
import com.banditgames.platform.player.adapter.web.dto.UpdatePlayerRequest;
import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.Rank;
import com.banditgames.platform.player.port.in.DeletePlayerUseCase;
import com.banditgames.platform.player.port.in.GetPlayerUseCase;
import com.banditgames.platform.player.port.in.SearchPlayersUseCase;
import com.banditgames.platform.player.port.in.UpdatePlayerUseCase;
import com.banditgames.platform.shared.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Players", description = "API for managing player profiles and information")
@SecurityRequirement(name = "bearerAuth")
public class PlayerController {

    private final GetPlayerUseCase getPlayerUseCase;
    private final UpdatePlayerUseCase updatePlayerUseCase;
    private final SearchPlayersUseCase searchPlayersUseCase;
    private final DeletePlayerUseCase deletePlayerUseCase;

    @Operation(
        summary = "Get authenticated player's profile",
        description = "Retrieves the full profile of the currently authenticated player, including private information such as email and address. " +
                "This endpoint returns the player's own profile data based on the JWT token provided in the request."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Player profile retrieved successfully",
            content = @Content(
                schema = @Schema(implementation = PlayerResponse.class),
                examples = @ExampleObject(
                    name = "Full player profile",
                    value = """
                        {
                          "playerId": "550e8400-e29b-41d4-a716-446655440000",
                          "username": "GamerPro2024",
                          "bio": "Competitive gamer who loves strategy games!",
                          "gamePreferences": [
                            "660e8400-e29b-41d4-a716-446655440001",
                            "770e8400-e29b-41d4-a716-446655440002"
                          ],
                          "email": "gamerpro@example.com",
                          "address": "123 Main St, New York, NY",
                          "rank": "GOLD",
                          "exp": 2500
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required",
            content = @Content(
                examples = @ExampleObject(
                    name = "Authentication error",
                    value = """
                        {
                          "message": "Authentication required"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Player profile not found",
            content = @Content(
                examples = @ExampleObject(
                    name = "Player not found",
                    value = """
                        {
                          "message": "Player not found with ID: 550e8400-e29b-41d4-a716-446655440000"
                        }
                        """
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<PlayerResponse> getAuthenticatedPlayer() {
        UUID userId = AuthUtils.getCurrentUserId();
        return getPlayerUseCase.getPlayer(userId)
                .map(PlayerResponse::fromDomain)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get authenticated player's friends",
            description = "Retrieves the authenticated player's friends details."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Player's friends profile details retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = PlayerResponse.class),
                            examples = @ExampleObject(
                                    name = "Full player profile",
                                    value = """
                        [
                            {
                              "playerId": "550e8400-e29b-41d4-a716-446655440000",
                              "username": "GamerPro2024",
                              "bio": "Competitive gamer who loves strategy games!",
                              "gamePreferences": [
                                "660e8400-e29b-41d4-a716-446655440001",
                                "770e8400-e29b-41d4-a716-446655440002"
                              ],
                              "rank": "GOLD",
                              "exp": 2500
                           }
                        ]
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - authentication required",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Authentication error",
                                    value = """
                        {
                          "message": "Authentication required"
                        }
                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Player profile not found",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Player not found",
                                    value = """
                        {
                          "message": "Player not found with ID: 550e8400-e29b-41d4-a716-446655440000"
                        }
                        """
                            )
                    )
            )
    })
    @GetMapping("/friends")
    public ResponseEntity<List<PlayerResponse>> getAuthenticatedPlayerFriends() {
        UUID userId = AuthUtils.getCurrentUserId();
        return ResponseEntity.ok(
                getPlayerUseCase.getPlayerFriends(userId).stream().map(PlayerResponse::fromDomain).toList()
        );
    }

    @Operation(
        summary = "Get player profile by ID",
        description = "Retrieves a specific player's public profile by their player ID. This endpoint returns public information only " +
                "and excludes private data such as email and address for privacy reasons. Use this endpoint to view other players' profiles."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Player profile retrieved successfully",
            content = @Content(
                schema = @Schema(implementation = PlayerResponse.class),
                examples = @ExampleObject(
                    name = "Public player profile",
                    value = """
                        {
                          "playerId": "660e8400-e29b-41d4-a716-446655440001",
                          "username": "ProStrategist",
                          "bio": "Chess master and puzzle enthusiast",
                          "gamePreferences": [
                            "770e8400-e29b-41d4-a716-446655440003"
                          ],
                          "email": null,
                          "address": null,
                          "rank": "PLATINUM",
                          "exp": 4200
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
            responseCode = "404",
            description = "Player not found with the specified ID",
            content = @Content(
                examples = @ExampleObject(
                    name = "Player not found",
                    value = """
                        {
                          "message": "Player not found"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/{playerId}")
    public ResponseEntity<PlayerResponse> getPlayerById(
            @Parameter(
                description = "Unique identifier of the player to retrieve",
                required = true,
                example = "660e8400-e29b-41d4-a716-446655440001",
                schema = @Schema(format = "uuid")
            )
            @PathVariable UUID playerId) {
        return getPlayerUseCase.getPlayer(playerId)
                .map(PlayerResponse::fromDomainWithoutPrivateInfo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Update authenticated player's profile",
        description = "Updates the profile information of the currently authenticated player. All fields in the request body are optional. " +
                "Only the provided fields will be updated, and null fields will be ignored. This endpoint allows players to modify their " +
                "username, bio, address, and game preferences. Note that rank and experience points cannot be modified through this endpoint " +
                "as they are managed by the system based on game outcomes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Player profile updated successfully",
            content = @Content(
                schema = @Schema(implementation = PlayerResponse.class),
                examples = @ExampleObject(
                    name = "Updated player profile",
                    value = """
                        {
                          "playerId": "550e8400-e29b-41d4-a716-446655440000",
                          "username": "UpdatedGamerName",
                          "bio": "Now focusing on puzzle games!",
                          "gamePreferences": [
                            "880e8400-e29b-41d4-a716-446655440005"
                          ],
                          "email": "gamerpro@example.com",
                          "address": "456 Oak Ave, Los Angeles, CA",
                          "rank": "GOLD",
                          "exp": 2500
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data - validation failed",
            content = @Content(
                examples = @ExampleObject(
                    name = "Validation error",
                    value = """
                        {
                          "message": "Username must be between 3 and 50 characters"
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
            responseCode = "404",
            description = "Player not found",
            content = @Content(
                examples = @ExampleObject(
                    name = "Player not found",
                    value = """
                        {
                          "message": "Player not found with ID: 550e8400-e29b-41d4-a716-446655440000"
                        }
                        """
                )
            )
        )
    })
    @PatchMapping
    public ResponseEntity<PlayerResponse> updatePlayer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Player profile update data. All fields are optional, and only provided fields will be updated. " +
                        "Validation rules: username (3-50 characters), bio (max 500 characters), address (max 255 characters).",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = UpdatePlayerRequest.class),
                    examples = {
                        @ExampleObject(
                            name = "Update username and bio",
                            value = """
                                {
                                  "username": "NewGamerName",
                                  "bio": "Competitive player looking for team!"
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Update game preferences",
                            value = """
                                {
                                  "gamePreferences": [
                                    "660e8400-e29b-41d4-a716-446655440001",
                                    "770e8400-e29b-41d4-a716-446655440002",
                                    "880e8400-e29b-41d4-a716-446655440005"
                                  ]
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Update all fields",
                            value = """
                                {
                                  "username": "ProGamer123",
                                  "bio": "Strategy game enthusiast",
                                  "address": "789 Elm St, Chicago, IL",
                                  "gamePreferences": [
                                    "660e8400-e29b-41d4-a716-446655440001"
                                  ]
                                }
                                """
                        )
                    }
                )
            )
            @Valid @RequestBody UpdatePlayerRequest request) {
        UUID userId = AuthUtils.getCurrentUserId();
        Player updated = updatePlayerUseCase.updatePlayer(
                userId,
                request.getUsername(),
                request.getBio(),
                request.getAddress(),
                request.getGamePreferences()
        );
        return ResponseEntity.ok(PlayerResponse.fromDomain(updated));
    }

    @Operation(
        summary = "Search for players",
        description = "Search and filter players based on various criteria. All query parameters are optional and can be combined. " +
                "When multiple parameters are provided, they are applied as AND conditions. This endpoint is useful for finding " +
                "players by username, filtering by rank, or discovering players who prefer specific games. The search is case-insensitive " +
                "for username queries and supports partial matching."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Players found successfully. Returns an array of player profiles matching the search criteria. " +
                    "Private information (email, address) is excluded from the results.",
            content = @Content(
                array = @ArraySchema(schema = @Schema(implementation = PlayerResponse.class)),
                examples = @ExampleObject(
                    name = "Search results",
                    value = """
                        [
                          {
                            "playerId": "550e8400-e29b-41d4-a716-446655440000",
                            "username": "GamerPro2024",
                            "bio": "Competitive gamer who loves strategy games!",
                            "gamePreferences": [
                              "660e8400-e29b-41d4-a716-446655440001"
                            ],
                            "email": null,
                            "address": null,
                            "rank": "GOLD",
                            "exp": 2500
                          },
                          {
                            "playerId": "660e8400-e29b-41d4-a716-446655440001",
                            "username": "GamerKing",
                            "bio": "Casual player",
                            "gamePreferences": [
                              "660e8400-e29b-41d4-a716-446655440001"
                            ],
                            "email": null,
                            "address": null,
                            "rank": "SILVER",
                            "exp": 1200
                          }
                        ]
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
            description = "Invalid query parameters - rank must be a valid enum value",
            content = @Content(
                examples = @ExampleObject(
                    name = "Invalid rank parameter",
                    value = """
                        {
                          "message": "Invalid rank value. Must be one of: BRONZE, SILVER, GOLD, PLATINUM, DIAMOND"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/search")
    public ResponseEntity<Page<PlayerResponse>> searchPlayers(
            @Parameter(
                description = "Filter by username (case-insensitive, partial match). Example: 'gamer' will match 'GamerPro', 'ProGamer', etc.",
                example = "gamer"
            )
            @RequestParam(required = false) String username,
            @Parameter(
                description = "Filter by player rank. Must be one of: BRONZE, SILVER, GOLD, PLATINUM, DIAMOND",
                example = "GOLD",
                schema = @Schema(allowableValues = {"BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND"})
            )
            @RequestParam(required = false) Rank rank,
            @Parameter(
                    description = "Filter by player rank. Must be one of: BRONZE, SILVER, GOLD, PLATINUM, DIAMOND",
                    example = "2")
            @PageableDefault(size = 20, sort = "username") Pageable pageable) {
        UUID userId = AuthUtils.getCurrentUserId();
        Page<Player> playersPage = searchPlayersUseCase.searchPlayersForUser(userId, username, rank, pageable);
        return ResponseEntity.ok(playersPage.map(PlayerResponse::fromDomainWithoutPrivateInfo));
    }

    @Operation(
        summary = "Delete authenticated player's profile",
        description = "Permanently deletes the profile of the currently authenticated player. This action cannot be undone. " +
                "All player data including username, bio, game preferences, and statistics will be permanently removed from the system. " +
                "The user's Keycloak account will remain active, but they will need to create a new player profile to use the platform again."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Player profile deleted successfully. No content is returned."
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Player not found",
            content = @Content(
                examples = @ExampleObject(
                    name = "Player not found",
                    value = """
                        {
                          "message": "Player not found with ID: 550e8400-e29b-41d4-a716-446655440000"
                        }
                        """
                )
            )
        )
    })
    @DeleteMapping
    public ResponseEntity<Void> deletePlayer() {
        UUID userId = AuthUtils.getCurrentUserId();
        deletePlayerUseCase.deletePlayer(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

