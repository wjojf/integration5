package com.banditgames.platform.achievements.adapter.web;

import com.banditgames.platform.achievements.adapter.web.dto.AchievementResponse;
import com.banditgames.platform.achievements.adapter.web.dto.UserAchievementResponse;
import com.banditgames.platform.achievements.domain.Achievement;
import com.banditgames.platform.achievements.domain.UserAchievement;
import com.banditgames.platform.achievements.port.in.GetAchievementsUseCase;
import com.banditgames.platform.achievements.port.in.GetUserAchievementsUseCase;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Achievements", description = "API for managing and retrieving achievements")
@SecurityRequirement(name = "bearerAuth")
public class AchievementController {

    private final GetAchievementsUseCase getAchievementsUseCase;
    private final GetUserAchievementsUseCase getUserAchievementsUseCase;

    @Operation(
        summary = "Get all achievements",
        description = "Retrieves all achievements available in the system, including both platform and third-party achievements. " +
                "Optionally filter by game ID using the query parameter. If no gameId is provided, returns all achievements " +
                "across all games. This endpoint is useful for browsing available achievements or viewing achievements for a specific game."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Achievements retrieved successfully",
            content = @Content(
                array = @ArraySchema(schema = @Schema(implementation = AchievementResponse.class)),
                examples = @ExampleObject(
                    name = "List of achievements",
                    value = """
                        [
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "gameId": "660e8400-e29b-41d4-a716-446655440001",
                            "name": "First Victory",
                            "description": "Win your first game",
                            "category": "PROGRESSION",
                            "rarity": "COMMON"
                          },
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440001",
                            "gameId": "660e8400-e29b-41d4-a716-446655440001",
                            "name": "Speed Demon",
                            "description": "Win a game in under 2 minutes",
                            "category": "TIME",
                            "rarity": "RARE"
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
        )
    })
    @GetMapping
    public ResponseEntity<List<AchievementResponse>> getAllAchievements(
            @Parameter(
                description = "Optional game ID to filter achievements by game. If not provided, returns all achievements.",
                required = false,
                example = "660e8400-e29b-41d4-a716-446655440001",
                schema = @Schema(format = "uuid")
            )
            @RequestParam(required = false) UUID gameId) {
        List<Achievement> achievements = getAchievementsUseCase.getAllAchievements(gameId);
        List<AchievementResponse> responses = achievements.stream()
                .map(AchievementResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get achievements for a specific game",
        description = "Retrieves all achievements available for a specific game, including both platform achievements and " +
                "third-party achievements. Third-party achievements are automatically included as they are associated with the game ID. " +
                "This endpoint is useful for displaying all available achievements for a particular game."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Game achievements retrieved successfully",
            content = @Content(
                array = @ArraySchema(schema = @Schema(implementation = AchievementResponse.class)),
                examples = @ExampleObject(
                    name = "List of game achievements",
                    value = """
                        [
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "gameId": "660e8400-e29b-41d4-a716-446655440001",
                            "name": "First Victory",
                            "description": "Win your first game",
                            "category": "PROGRESSION",
                            "rarity": "COMMON"
                          },
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440001",
                            "gameId": "660e8400-e29b-41d4-a716-446655440001",
                            "name": "Speed Demon",
                            "description": "Win a game in under 2 minutes",
                            "category": "TIME",
                            "rarity": "RARE"
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
        )
    })
    @GetMapping("/games/{gameId}")
    public ResponseEntity<List<AchievementResponse>> getGameAchievements(
            @Parameter(
                description = "Unique identifier of the game whose achievements to retrieve",
                required = true,
                example = "660e8400-e29b-41d4-a716-446655440001",
                schema = @Schema(format = "uuid")
            )
            @PathVariable UUID gameId) {
        List<Achievement> achievements = getAchievementsUseCase.getAllAchievements(gameId);
        List<AchievementResponse> responses = achievements.stream()
                .map(AchievementResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(
        summary = "Get user achievements by user ID",
        description = "Retrieves all achievements unlocked by a specific user across all games they have played. " +
                "This combines achievements from all games, including both platform and third-party achievements. " +
                "Optionally filter by game ID using the query parameter to get achievements for a specific game only. " +
                "When no gameId is provided, returns all achievements unlocked by the user across all games. " +
                "This endpoint allows viewing a user's complete achievement progress and can be used to display achievement badges or progress tracking."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User achievements retrieved successfully",
            content = @Content(
                array = @ArraySchema(schema = @Schema(implementation = UserAchievementResponse.class)),
                examples = @ExampleObject(
                    name = "List of user achievements",
                    value = """
                        [
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "userId": "660e8400-e29b-41d4-a716-446655440001",
                            "achievementId": "770e8400-e29b-41d4-a716-446655440002",
                            "unlockedAt": "2024-01-15T10:30:00"
                          },
                          {
                            "id": "550e8400-e29b-41d4-a716-446655440003",
                            "userId": "660e8400-e29b-41d4-a716-446655440001",
                            "achievementId": "770e8400-e29b-41d4-a716-446655440004",
                            "unlockedAt": "2024-01-20T14:45:00"
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
            description = "User not found",
            content = @Content(
                examples = @ExampleObject(
                    name = "User not found",
                    value = """
                        {
                          "message": "User not found with ID: 660e8400-e29b-41d4-a716-446655440001"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<UserAchievementResponse>> getUserAchievements(
            @Parameter(
                description = "Unique identifier of the user whose achievements to retrieve",
                required = true,
                example = "660e8400-e29b-41d4-a716-446655440001",
                schema = @Schema(format = "uuid")
            )
            @PathVariable UUID userId,
            @Parameter(
                description = "Optional game ID to filter achievements by game. If not provided, returns all achievements for the user.",
                required = false,
                example = "770e8400-e29b-41d4-a716-446655440002",
                schema = @Schema(format = "uuid")
            )
            @RequestParam(required = false) UUID gameId) {
        List<UserAchievement> userAchievements = getUserAchievementsUseCase.getUserAchievements(userId, gameId);
        List<UserAchievementResponse> responses = userAchievements.stream()
                .map(UserAchievementResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

