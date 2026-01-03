package com.banditgames.gateway.filter;

import com.banditgames.gateway.config.GatewayConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Anti-Corruption Layer (ACL) filter for external game integration.
 *
 * Transforms requests/responses between our internal platform format and external game service formats.
 *
 * Flow:
 * 1. Our platform calls: POST /api/external/chess/games/{gameId} (our internal format)
 * 2. ACL transforms request: Our format → External chess service format
 * 3. Calls external chess service: POST /api/games/{gameId} (external format)
 * 4. ACL transforms response: External format → Our internal format
 * 5. Returns to caller in our internal format
 */
@Slf4j
@Component
public class ACLTransformFilter extends AbstractGatewayFilterFactory<ACLTransformFilter.Config> {

    private static final Set<String> METHODS_WITH_BODY = Set.of(
        GatewayConstants.HTTP_METHOD_POST,
        GatewayConstants.HTTP_METHOD_PUT,
        GatewayConstants.HTTP_METHOD_PATCH
    );

    private final ObjectMapper objectMapper;
    private final ChessGameTransformer chessTransformer;

    public ACLTransformFilter(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
        this.chessTransformer = new ChessGameTransformer();
    }

    @Override
    public GatewayFilter apply(Config config) {
        if (!config.isEnabled()) {
            return (exchange, chain) -> chain.filter(exchange);
        }

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // ACL transformation is only applied to chess game requests
            // All other requests are passed through unchanged (full proxy behavior)
            if (path.contains("/api/external/chess/")) {
                log.debug("ACL: Applying transformation for chess game request: {}", path);
                return handleChessGameRequest(exchange, chain);
            }

            // Default: Full proxy - pass through unchanged
            log.debug("ACL: Passing through request (no transformation): {}", path);
            return chain.filter(exchange);
        };
    }

    private Mono<Void> handleChessGameRequest(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Transform request if it has a body
        if (METHODS_WITH_BODY.contains(request.getMethod().name())) {
            return transformRequestAndResponse(exchange, chain);
        } else {
            // For GET requests, only transform response
            return transformResponse(exchange, chain);
        }
    }

    private Mono<Void> transformRequestAndResponse(ServerWebExchange exchange, GatewayFilterChain chain) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .flatMap(dataBuffer -> {
                try {
                    // 1. Read our internal format request
                    String body = readRequestBody(dataBuffer);
                    Map<String, Object> internalRequest = parseJson(body);

                    // 2. Transform to external chess service format
                    Map<String, Object> externalRequest = chessTransformer.transformRequestToExternal(internalRequest);

                    // 3. Create transformed request
                    byte[] transformedBytes = objectMapper.writeValueAsBytes(externalRequest);
                    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(transformedBytes);

                    ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(buffer);
                        }
                    };

                    log.debug("ACL: Transformed request from internal to external chess format");

                    // 4. Continue with transformed request and transform response
                    ServerWebExchange mutatedExchange = exchange.mutate().request(requestDecorator).build();
                    return transformResponse(mutatedExchange, chain);

                } catch (Exception e) {
                    log.error("ACL: Error transforming chess game request", e);
                    return Mono.error(e);
                }
            });
    }

    private Mono<Void> transformResponse(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<DataBuffer> fluxBody = (Flux<DataBuffer>) body;
                    // Join all DataBuffers into one to handle multiple buffers correctly
                    return DataBufferUtils.join(fluxBody)
                        .flatMap(dataBuffer -> {
                            String responseBody = null;
                            try {
                                // 1. Read external chess service response
                                responseBody = readRequestBody(dataBuffer);
                                
                                // Handle empty response - pass through as-is
                                if (responseBody == null || responseBody.trim().isEmpty()) {
                                    log.warn("ACL: Empty response body, passing through");
                                    DataBuffer emptyBuffer = originalResponse.bufferFactory().wrap(new byte[0]);
                                    return super.writeWith(Flux.just(emptyBuffer));
                                }
                                
                                Map<String, Object> externalResponse = parseJson(responseBody);

                                // 2. Transform to our internal format
                                Map<String, Object> internalResponse = chessTransformer.transformResponseToInternal(externalResponse);

                                // 3. Write transformed response
                                byte[] transformedBytes = objectMapper.writeValueAsBytes(internalResponse);
                                DataBuffer buffer = originalResponse.bufferFactory().wrap(transformedBytes);
                                
                                // Set Content-Type header if not already set
                                if (!originalResponse.getHeaders().containsKey("Content-Type")) {
                                    originalResponse.getHeaders().add("Content-Type", "application/json");
                                }

                                log.debug("ACL: Transformed response from external chess to internal format");
                                return super.writeWith(Flux.just(buffer));

                            } catch (Exception e) {
                                log.error("ACL: Error transforming chess game response: {}", e.getMessage(), e);
                                // If transformation fails, try to return original response
                                if (responseBody != null) {
                                    try {
                                        DataBuffer errorBuffer = originalResponse.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
                                        return super.writeWith(Flux.just(errorBuffer));
                                    } catch (Exception ex) {
                                        log.error("ACL: Failed to recreate response buffer", ex);
                                    }
                                }
                                // Last resort: return empty response
                                DataBuffer emptyBuffer = originalResponse.bufferFactory().wrap(new byte[0]);
                                return super.writeWith(Flux.just(emptyBuffer));
                            }
                        })
                        .onErrorResume(error -> {
                            log.error("ACL: Error processing response body", error);
                            // Fallback: return empty response
                            DataBuffer emptyBuffer = originalResponse.bufferFactory().wrap(new byte[0]);
                            return super.writeWith(Flux.just(emptyBuffer));
                        });
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(responseDecorator).build());
    }

    private String readRequestBody(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String body) throws Exception {
        if (body == null || body.trim().isEmpty()) {
            return new java.util.HashMap<>();
        }
        return objectMapper.readValue(body, Map.class);
    }

    /**
     * Transforms between our internal platform format and external chess service format.
     *
     * External chess service format (from docs/chess.md):
     * - POST /api/games/{gameId}: { whitePlayer, blackPlayer, currentFen, status }
     * - POST /api/moves/{gameId}: { fromSquare, toSquare, sanNotation, player, ... }
     *
     * Our internal format:
     * - POST /api/external/chess/games/{gameId}: { sessionId, gameType, playerIds, configuration }
     * - POST /api/external/chess/moves/{gameId}: { playerId, move: { from, to, san }, ... }
     */
    private static class ChessGameTransformer {

        /**
         * Transforms our internal request format to external chess service format.
         */
        @SuppressWarnings("unchecked")
        Map<String, Object> transformRequestToExternal(Map<String, Object> internalRequest) {
            log.debug("ACL: Transforming internal request to external chess format");

            // Detect request type by checking path or request structure
            if (internalRequest.containsKey("playerIds") || internalRequest.containsKey("sessionId")) {
                // This is a game creation/update request
                return transformGameRequestToExternal(internalRequest);
            } else if (internalRequest.containsKey("move") || internalRequest.containsKey("playerId")) {
                // This is a move request
                return transformMoveRequestToExternal(internalRequest);
            } else if (internalRequest.containsKey("frontendUrl")) {
                // This is a registration request
                return internalRequest; // Registration format is the same
            }

            // Default: pass through (might be external format already)
            return internalRequest;
        }

        /**
         * Transforms game creation/update request from internal to external format.
         */
        @SuppressWarnings("unchecked")
        private Map<String, Object> transformGameRequestToExternal(Map<String, Object> internal) {
            Map<String, Object> external = new java.util.HashMap<>();

            // Extract from internal format
            Map<String, Object> config = (Map<String, Object>) internal.getOrDefault("configuration", new java.util.HashMap<>());

            // Map to external chess service format
            external.put("whitePlayer", config.getOrDefault("whitePlayer", internal.get("whitePlayer")));
            external.put("blackPlayer", config.getOrDefault("blackPlayer", internal.get("blackPlayer")));
            external.put("currentFen", config.getOrDefault("initialFen", config.getOrDefault("currentFen",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")));
            external.put("status", config.getOrDefault("status", internal.getOrDefault("status", "ACTIVE")));

            // If playerIds are provided, we'd need to resolve them to names (handled by platform-backend)
            // For now, assume whitePlayer/blackPlayer are already in the config

            return external;
        }

        /**
         * Transforms move request from internal to external format.
         */
        @SuppressWarnings("unchecked")
        private Map<String, Object> transformMoveRequestToExternal(Map<String, Object> internal) {
            Map<String, Object> external = new java.util.HashMap<>();

            // Extract move from internal format
            Map<String, Object> move = (Map<String, Object>) internal.getOrDefault("move", new java.util.HashMap<>());

            // Map to external chess service format
            external.put("fromSquare", move.getOrDefault("from", internal.get("fromSquare")));
            external.put("toSquare", move.getOrDefault("to", internal.get("toSquare")));
            external.put("sanNotation", move.getOrDefault("san", internal.get("sanNotation")));
            external.put("player", internal.getOrDefault("player", "WHITE")); // WHITE or BLACK

            // Copy other fields if present
            if (internal.containsKey("fenAfterMove")) {
                external.put("fenAfterMove", internal.get("fenAfterMove"));
            }
            if (internal.containsKey("moveNumber")) {
                external.put("moveNumber", internal.get("moveNumber"));
            }

            return external;
        }

        /**
         * Transforms external chess service response to our internal format.
         */
        @SuppressWarnings("unchecked")
        Map<String, Object> transformResponseToInternal(Map<String, Object> externalResponse) {
            log.debug("ACL: Transforming external chess response to internal format");

            // External chess service responses typically contain:
            // - Game data: { gameId, whitePlayer, blackPlayer, currentFen, status, ... }
            // - Move response: { success, gameState, ... }

            Map<String, Object> internal = new java.util.HashMap<>();

            // Copy common fields
            if (externalResponse.containsKey("gameId")) {
                internal.put("sessionId", externalResponse.get("gameId"));
                internal.put("gameId", externalResponse.get("gameId"));
            }

            // Transform game state
            if (externalResponse.containsKey("currentFen") || externalResponse.containsKey("whitePlayer")) {
                // This is a game state response
                internal.put("gameType", "chess");
                internal.put("status", externalResponse.getOrDefault("status", "ACTIVE"));

                Map<String, Object> config = new java.util.HashMap<>();
                config.put("whitePlayer", externalResponse.get("whitePlayer"));
                config.put("blackPlayer", externalResponse.get("blackPlayer"));
                config.put("currentFen", externalResponse.get("currentFen"));
                internal.put("configuration", config);

                // Copy other fields
                if (externalResponse.containsKey("winner")) {
                    internal.put("winner", externalResponse.get("winner"));
                }
                if (externalResponse.containsKey("endReason")) {
                    internal.put("endReason", externalResponse.get("endReason"));
                }
            } else {
                // Generic response (e.g., move success, registration)
                internal.putAll(externalResponse);
            }

            return internal;
        }
    }

    public static class Config {
        private boolean enabled = true;
        private String direction = "request"; // request, response, both

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }
}
