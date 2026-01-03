package com.banditgames.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

/**
 * Filter to generate a gameId (UUID) according to platform rules and rewrite the path.
 * 
 * This filter:
 * 1. Generates a UUID as gameId according to platform rules
 * 2. Rewrites the path to include the generated gameId
 * 
 * Used for: POST /api/external/chess/register
 * Transforms to: POST /api/platform/register/{generatedGameId}
 */
@Slf4j
@Component
public class GenerateGameIdFilter extends AbstractGatewayFilterFactory<GenerateGameIdFilter.Config> {

    private static final String GAME_ID_ATTRIBUTE = "generatedGameId";

    public GenerateGameIdFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        if (!config.isEnabled()) {
            return (exchange, chain) -> chain.filter(exchange);
        }

        return (exchange, chain) -> {
            // Generate gameId according to platform rules (UUID)
            UUID gameId = UUID.randomUUID();
            String gameIdStr = gameId.toString();
            
            // Store in attributes for potential use by other filters
            exchange.getAttributes().put(GAME_ID_ATTRIBUTE, gameIdStr);
            
            // Get current request path
            String originalPath = exchange.getRequest().getURI().getPath();
            
            // Rewrite path: /api/external/chess/register -> /api/external/chess/register/{gameId}
            // This routes to platform-backend which handles the registration
            String newPath = "/api/external/chess/register/" + gameIdStr;
            
            // Build new URI with rewritten path
            URI newUri = exchange.getRequest().getURI().resolve(newPath);
            
            // Create mutated request with new path
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .uri(newUri)
                    .header("X-Generated-Game-Id", gameIdStr)
                    .build();
            
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutatedRequest)
                    .build();
            
            log.debug("Generated gameId for registration: {} - Rewritten path: {} -> {}", 
                    gameId, originalPath, newPath);
            
            return chain.filter(mutatedExchange);
        };
    }

    /**
     * Get the generated gameId from exchange attributes.
     * Used by other filters that need the gameId.
     */
    public static String getGameId(ServerWebExchange exchange) {
        return (String) exchange.getAttributes().get(GAME_ID_ATTRIBUTE);
    }

    public static class Config {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}

