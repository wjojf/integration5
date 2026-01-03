package com.banditgames.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service registry configuration.
 * Maps route IDs and path patterns to service names for dynamic service resolution.
 * 
 * This allows the gateway to scale to many services without hardcoding service names.
 * 
 * Configuration can be provided via application.yml:
 * gateway:
 *   services:
 *     route-id-to-service-name:
 *       platform-backend: platform-backend
 *     path-prefix-to-service-name:
 *       /api/platform: platform-backend
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gateway.services")
public class ServiceRegistry {

    /**
     * Map of route ID to service name.
     * Example: "platform-backend" -> "platform-backend"
     */
    private Map<String, String> routeIdToServiceName = new HashMap<>();

    /**
     * Map of path prefix to service name.
     * Example: "/api/platform" -> "platform-backend"
     */
    private Map<String, String> pathPrefixToServiceName = new HashMap<>();

    /**
     * Map of route ID prefix to service name (for routes with dynamic IDs).
     * Example: "external-chess" -> "chess-service"
     */
    private Map<String, String> routeIdPrefixToServiceName = new HashMap<>();

    /**
     * Resolve service name from route ID.
     */
    public Optional<String> resolveFromRouteId(String routeId) {
        if (routeId == null) {
            return Optional.empty();
        }

        // Exact match
        if (routeIdToServiceName.containsKey(routeId)) {
            return Optional.of(routeIdToServiceName.get(routeId));
        }

        // Prefix match
        return routeIdPrefixToServiceName.entrySet().stream()
            .filter(entry -> routeId.startsWith(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    /**
     * Resolve service name from request path.
     */
    public Optional<String> resolveFromPath(String path) {
        if (path == null) {
            return Optional.empty();
        }

        return pathPrefixToServiceName.entrySet().stream()
            .filter(entry -> path.startsWith(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    /**
     * Initialize default mappings (can be overridden via configuration).
     * This is called after properties are bound.
     */
    @PostConstruct
    public void initializeDefaults() {
        // Only initialize if not already set via configuration
        if (routeIdToServiceName.isEmpty()) {
            // Route ID mappings
            routeIdToServiceName.put("platform-backend", "platform-backend");
            routeIdToServiceName.put("game-service", "game-service");
            routeIdToServiceName.put("external-games", "external-games");
        }

        if (routeIdPrefixToServiceName.isEmpty()) {
            // Route ID prefix mappings
            routeIdPrefixToServiceName.put("external-chess", "chess-service");
        }

        if (pathPrefixToServiceName.isEmpty()) {
            // Path prefix mappings
            pathPrefixToServiceName.put("/api/platform", "platform-backend");
            pathPrefixToServiceName.put("/api/games", "game-service");
            pathPrefixToServiceName.put("/api/external/chess", "chess-service");
            pathPrefixToServiceName.put("/api/external", "external-games");
        }
    }
}
