package com.banditgames.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Controller for serving aggregated API documentation.
 * Aggregates OpenAPI specs from all backend services and gateway routes.
 */
@Slf4j
@RestController
@RequestMapping("/api/docs")
public class GatewayDocumentationController {

    private final WebClient webClient;
    private final String gameServiceUrl;
    private final String platformBackendUrl;
    private final String gatewayBaseUrl;

    public GatewayDocumentationController(
            WebClient.Builder webClientBuilder,
            @Value("${GAME_SERVICE_URL:http://localhost:8000}") String gameServiceUrl,
            @Value("${PLATFORM_BACKEND_URL:http://localhost:8081}") String platformBackendUrl,
            @Value("${server.port:8080}") String serverPort) {
        this.webClient = webClientBuilder.build();
        this.gameServiceUrl = gameServiceUrl;
        this.platformBackendUrl = platformBackendUrl;
        this.gatewayBaseUrl = "http://localhost:" + serverPort;
    }

    /**
     * Get aggregated OpenAPI documentation from all services.
     * Merges paths from game-service, platform-backend, and gateway routes.
     */
    @GetMapping("/openapi.json")
    public Mono<ResponseEntity<Map<String, Object>>> getAggregatedOpenAPI() {
        return Mono.zip(
            fetchGameServiceOpenAPI(),
            fetchPlatformBackendOpenAPI()
        ).map(tuple -> {
            Map<String, Object> gameServiceAPI = tuple.getT1();
            Map<String, Object> platformBackendAPI = tuple.getT2();
            
            // Create aggregated OpenAPI as Map
            Map<String, Object> aggregated = new HashMap<>();
            aggregated.put("openapi", "3.0.3");
            
            // Set info
            Map<String, Object> info = new HashMap<>();
            info.put("title", "BanditGames Platform - Aggregated API");
            info.put("version", "1.0.0");
            info.put("description", """
                Complete API documentation for BanditGames Platform.
                
                This aggregated API includes:
                - **API Gateway Routes**: Centralized routing and ACL transformation
                - **Platform Backend**: Lobbies, friends, achievements, game registry
                - **Game Service**: Game engine, AI player, ML models, chatbot, game logging
                - **External Games**: Chess game integration via ACL
                
                **Authentication:**
                All requests (except public endpoints) require a valid JWT token from Keycloak.
                Include the token in the Authorization header: `Authorization: Bearer <token>`
                
                **Rate Limiting:**
                Different endpoints have different rate limits based on their usage patterns.
                
                **Service-to-Service Communication:**
                Internal services communicate using API keys via the `X-API-Key` header.
                """);
            aggregated.put("info", info);
            
            // Set servers
            List<Map<String, String>> servers = List.of(
                Map.of("url", gatewayBaseUrl, "description", "API Gateway (Local Development)"),
                Map.of("url", "https://api.banditgames.com", "description", "Production Server")
            );
            aggregated.put("servers", servers);
            
            // Merge paths
            Map<String, Object> paths = new HashMap<>();
            
            // Add gateway-specific paths
            addGatewayPathsToMap(paths);
            
            // Add game-service paths (with /api/games prefix)
            if (gameServiceAPI != null && gameServiceAPI.containsKey("paths")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> gameServicePaths = (Map<String, Object>) gameServiceAPI.get("paths");
                if (gameServicePaths != null) {
                    gameServicePaths.forEach((path, pathItem) -> {
                        // Transform /api/v1/* to /api/games/*
                        String gatewayPath = path.replace("/api/v1/", "/api/games/");
                        paths.put(gatewayPath, pathItem);
                    });
                }
            }
            
            // Add platform-backend paths
            if (platformBackendAPI != null && platformBackendAPI.containsKey("paths")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> platformPaths = (Map<String, Object>) platformBackendAPI.get("paths");
                if (platformPaths != null) {
                    platformPaths.forEach((path, pathItem) -> {
                        paths.put(path, pathItem);
                    });
                }
            }
            
            // Add external game paths
            addExternalGamePathsToMap(paths);
            
            aggregated.put("paths", paths);
            
            // Merge components (schemas, security schemes, etc.)
            Map<String, Object> components = new HashMap<>();
            
            if (gameServiceAPI != null && gameServiceAPI.containsKey("components")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> gameComponents = (Map<String, Object>) gameServiceAPI.get("components");
                if (gameComponents != null) {
                    components.putAll(gameComponents);
                }
            }
            
            if (platformBackendAPI != null && platformBackendAPI.containsKey("components")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> platformComponents = (Map<String, Object>) platformBackendAPI.get("components");
                if (platformComponents != null) {
                    // Merge schemas
                    if (platformComponents.containsKey("schemas")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> platformSchemas = (Map<String, Object>) platformComponents.get("schemas");
                        if (!components.containsKey("schemas")) {
                            components.put("schemas", new HashMap<>());
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mergedSchemas = (Map<String, Object>) components.get("schemas");
                        if (platformSchemas != null) {
                            mergedSchemas.putAll(platformSchemas);
                        }
                    }
                    // Merge security schemes if present
                    if (platformComponents.containsKey("securitySchemes")) {
                        if (!components.containsKey("securitySchemes")) {
                            components.put("securitySchemes", new HashMap<>());
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> platformSecurity = (Map<String, Object>) platformComponents.get("securitySchemes");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> mergedSecurity = (Map<String, Object>) components.get("securitySchemes");
                        if (platformSecurity != null) {
                            mergedSecurity.putAll(platformSecurity);
                        }
                    }
                }
            }
            
            if (!components.isEmpty()) {
                aggregated.put("components", components);
            }
            
            return ResponseEntity.ok(aggregated);
        }).onErrorResume(error -> {
            log.error("Failed to aggregate OpenAPI documentation", error);
            // Return a basic OpenAPI spec even on error
            Map<String, Object> fallback = Map.of(
                "openapi", "3.0.3",
                "info", Map.of(
                    "title", "BanditGames Platform - API Documentation",
                    "version", "1.0.0",
                    "description", "Failed to aggregate documentation: " + error.getMessage()
                ),
                "paths", Map.of()
            );
            return Mono.just(ResponseEntity.ok(fallback));
        });
    }

    /**
     * Add gateway-specific paths to the OpenAPI spec (as Map).
     */
    private void addGatewayPathsToMap(Map<String, Object> paths) {
        // Health check
        Map<String, Object> healthGet = Map.of(
            "summary", "Gateway health check",
            "description", "Check if the API Gateway is healthy",
            "operationId", "gatewayHealth",
            "tags", List.of("Gateway"),
            "responses", Map.of(
                "200", Map.of("description", "Gateway is healthy")
            )
        );
        paths.put("/health", Map.of("get", healthGet));
        
        // Documentation endpoints
        Map<String, Object> docsGet = Map.of(
            "summary", "Get aggregated API documentation",
            "description", "Returns aggregated OpenAPI documentation from all services",
            "operationId", "getAggregatedDocs",
            "tags", List.of("Documentation"),
            "responses", Map.of(
                "200", Map.of(
                    "description", "Aggregated OpenAPI documentation",
                    "content", Map.of(
                        "application/json", Map.of(
                            "schema", Map.of("type", "object")
                        )
                    )
                )
            )
        );
        paths.put("/api/docs/openapi.json", Map.of("get", docsGet));
        
        Map<String, Object> aggregateGet = Map.of(
            "summary", "Get aggregated documentation summary",
            "description", "Returns structured documentation summary from all services",
            "operationId", "getAggregatedDocsSummary",
            "tags", List.of("Documentation"),
            "responses", Map.of(
                "200", Map.of("description", "Documentation summary")
            )
        );
        paths.put("/api/docs/aggregate", Map.of("get", aggregateGet));
    }

    /**
     * Add external game paths to the OpenAPI spec (as Map).
     */
    private void addExternalGamePathsToMap(Map<String, Object> paths) {
        // Chess game registration
        Map<String, Object> registerPost = Map.of(
            "summary", "Register chess game",
            "description", "Register external chess game with the platform. GameId is generated by gateway (UUID).",
            "operationId", "registerChessGame",
            "tags", List.of("External Games - Chess"),
            "requestBody", Map.of(
                "required", true,
                "content", Map.of(
                    "application/json", Map.of(
                        "schema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "frontendUrl", Map.of("type", "string", "description", "Frontend URL for the chess game"),
                                "pictureUrl", Map.of("type", "string", "description", "Picture URL for the chess game")
                            ),
                            "required", List.of("frontendUrl")
                        )
                    )
                )
            ),
            "responses", Map.of(
                "201", Map.of("description", "Chess game registered successfully"),
                "400", Map.of("description", "Invalid request"),
                "500", Map.of("description", "Internal server error")
            )
        );
        paths.put("/api/external/chess/register", Map.of("post", registerPost));
        
        // Chess game sessions
        Map<String, Object> gameGet = Map.of(
            "summary", "Get chess game",
            "description", "Get chess game information",
            "operationId", "getChessGame",
            "tags", List.of("External Games - Chess"),
            "parameters", List.of(
                Map.of(
                    "name", "gameId",
                    "in", "path",
                    "required", true,
                    "schema", Map.of("type", "string", "format", "uuid"),
                    "description", "Chess game ID"
                )
            ),
            "responses", Map.of(
                "200", Map.of("description", "Chess game information"),
                "404", Map.of("description", "Game not found")
            )
        );
        
        Map<String, Object> gamePost = Map.of(
            "summary", "Create/update chess game",
            "description", "Create or update a chess game session",
            "operationId", "createChessGame",
            "tags", List.of("External Games - Chess"),
            "parameters", List.of(
                Map.of(
                    "name", "gameId",
                    "in", "path",
                    "required", true,
                    "schema", Map.of("type", "string", "format", "uuid"),
                    "description", "Chess game ID"
                )
            ),
            "requestBody", Map.of(
                "required", true,
                "content", Map.of(
                    "application/json", Map.of(
                        "schema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "whitePlayer", Map.of("type", "string"),
                                "blackPlayer", Map.of("type", "string"),
                                "currentFen", Map.of("type", "string"),
                                "status", Map.of("type", "string", "enum", List.of("ACTIVE", "FINISHED"))
                            )
                        )
                    )
                )
            ),
            "responses", Map.of(
                "200", Map.of("description", "Chess game created/updated"),
                "400", Map.of("description", "Invalid request")
            )
        );
        paths.put("/api/external/chess/games/{gameId}", Map.of("get", gameGet, "post", gamePost));
        
        // Chess moves
        Map<String, Object> movePost = Map.of(
            "summary", "Make chess move",
            "description", "Make a move in a chess game",
            "operationId", "makeChessMove",
            "tags", List.of("External Games - Chess"),
            "parameters", List.of(
                Map.of(
                    "name", "gameId",
                    "in", "path",
                    "required", true,
                    "schema", Map.of("type", "string", "format", "uuid"),
                    "description", "Chess game ID"
                )
            ),
            "requestBody", Map.of(
                "required", true,
                "content", Map.of(
                    "application/json", Map.of(
                        "schema", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "fromSquare", Map.of("type", "string", "example", "e2"),
                                "toSquare", Map.of("type", "string", "example", "e4"),
                                "player", Map.of("type", "string", "enum", List.of("WHITE", "BLACK"))
                            ),
                            "required", List.of("fromSquare", "toSquare", "player")
                        )
                    )
                )
            ),
            "responses", Map.of(
                "200", Map.of("description", "Move applied successfully"),
                "400", Map.of("description", "Invalid move"),
                "404", Map.of("description", "Game not found")
            )
        );
        paths.put("/api/external/chess/moves/{gameId}", Map.of("post", movePost));
    }

    private Mono<Map<String, Object>> fetchGameServiceOpenAPI() {
        return webClient.get()
            .uri(gameServiceUrl + "/openapi.json")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .onErrorResume(error -> {
                log.warn("Failed to fetch game-service OpenAPI: {}", error.getMessage());
                return Mono.just(Map.of("paths", Map.of(), "components", Map.of()));
            });
    }

    private Mono<Map<String, Object>> fetchPlatformBackendOpenAPI() {
        return webClient.get()
            .uri(platformBackendUrl + "/v3/api-docs")
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .onErrorResume(error -> {
                log.warn("Failed to fetch platform-backend OpenAPI: {}", error.getMessage());
                return Mono.just(Map.of("paths", Map.of(), "components", Map.of()));
            });
    }

    @GetMapping("/aggregate")
    public Mono<ResponseEntity<Map<String, Object>>> getAggregatedDocs() {
        return Mono.zip(
            fetchGameServiceDocs(),
            fetchPlatformBackendDocs()
        ).map(tuple -> {
            Map<String, Object> aggregated = new HashMap<>();
            aggregated.put("game-service", tuple.getT1());
            aggregated.put("platform-backend", tuple.getT2());
            aggregated.put("gateway", Map.of(
                "name", "API Gateway",
                "version", "1.0.0",
                "description", "Centralized API Gateway for BanditGames Platform"
            ));
            return ResponseEntity.ok(aggregated);
        }).onErrorResume(error -> {
            log.error("Failed to aggregate API documentation", error);
            return Mono.just(ResponseEntity.ok(Map.of(
                "error", "Failed to aggregate documentation",
                "message", error.getMessage()
            )));
        });
    }

    private Mono<Map<String, Object>> fetchGameServiceDocs() {
        return webClient.get()
            .uri(gameServiceUrl + "/openapi.json")
            .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
            .onErrorResume(error -> {
                log.warn("Failed to fetch game-service docs", error);
                return Mono.just(Map.of("error", "Service unavailable"));
            });
    }

    private Mono<Map<String, Object>> fetchPlatformBackendDocs() {
        return webClient.get()
            .uri(platformBackendUrl + "/v3/api-docs")
            .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
            .onErrorResume(error -> {
                log.warn("Failed to fetch platform-backend docs: {}", error.getMessage());
                return Mono.just(Map.of("error", "Service unavailable", "details", error.getMessage()));
            });
    }
}
