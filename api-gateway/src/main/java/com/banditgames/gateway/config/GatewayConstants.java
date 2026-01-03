package com.banditgames.gateway.config;

/**
 * Constants for API Gateway configuration.
 * Centralizes all string literals to avoid raw strings in code.
 */
public final class GatewayConstants {

    private GatewayConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Route IDs
    public static final String ROUTE_ID_PLATFORM_BACKEND = "platform-backend";
    public static final String ROUTE_ID_GAME_SERVICE = "game-service";
    public static final String ROUTE_ID_EXTERNAL_GAMES = "external-games";

    // Path Patterns
    public static final String PATH_PATTERN_PLATFORM = "/api/platform/**";
    public static final String PATH_PATTERN_GAMES = "/api/games/**";
    public static final String PATH_PATTERN_EXTERNAL = "/api/**";

    // Service URIs
    public static final String URI_PLATFORM_BACKEND = "http://platform-backend:8081";
    public static final String URI_GAME_SERVICE = "http://game-service:8000";

    // Filter Names
    public static final String FILTER_STRIP_PREFIX = "StripPrefix";
    public static final String FILTER_REQUEST_RATE_LIMITER = "RequestRateLimiter";
    public static final String FILTER_CIRCUIT_BREAKER = "CircuitBreaker";
    public static final String FILTER_ACL_TRANSFORM = "ACLTransform";

    // Circuit Breaker Names
    public static final String CIRCUIT_BREAKER_PLATFORM_BACKEND = "platformBackend";
    public static final String CIRCUIT_BREAKER_GAME_SERVICE = "gameService";

    // Fallback URIs
    public static final String FALLBACK_URI_PLATFORM = "forward:/fallback/platform";
    public static final String FALLBACK_URI_GAME = "forward:/fallback/game";

    // Header Names
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    // JWT Claims
    public static final String JWT_CLAIM_SUB = "sub";
    public static final String JWT_CLAIM_EMAIL = "email";
    public static final String JWT_CLAIM_PREFERRED_USERNAME = "preferred_username";
    public static final String JWT_CLAIM_REALM_ACCESS_ROLES = "realm_access.roles";

    // Public Endpoints
    public static final String PATH_ACTUATOR = "/actuator/**";
    public static final String PATH_HEALTH = "/health";
    public static final String PATH_FALLBACK = "/fallback/**";
    public static final String PATH_GAMES = "/api/v1/games/games";

    // HTTP Methods
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_PATCH = "PATCH";
}
