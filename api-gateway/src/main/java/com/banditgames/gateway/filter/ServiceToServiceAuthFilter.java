package com.banditgames.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Service-to-service authentication filter.
 * Validates API key for internal service communication.
 */
@Slf4j
@Component
public class ServiceToServiceAuthFilter implements GlobalFilter, Ordered {

    private static final int FILTER_ORDER = -50;
    private static final String HEADER_API_KEY = "X-API-Key";
    private static final String HEADER_SERVICE_NAME = "X-Service-Name";

    @Value("${SERVICE_API_KEY:dev-key}")
    private String expectedApiKey;

    @Value("${GATEWAY_SERVICE_NAME:api-gateway}")
    private String gatewayServiceName;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip auth for public endpoints
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // Check if request is from internal service (has API key)
        String apiKey = request.getHeaders().getFirst(HEADER_API_KEY);
        String serviceName = request.getHeaders().getFirst(HEADER_SERVICE_NAME);

        // If API key is present, validate it
        if (apiKey != null) {
            if (!isValidApiKey(apiKey)) {
                log.warn("Invalid API key from service: {}", serviceName);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            log.debug("Validated API key from service: {}", serviceName);
        }

        // Add gateway service name to request for downstream services
        ServerHttpRequest enrichedRequest = request.mutate()
                .header(HEADER_SERVICE_NAME, gatewayServiceName)
                .build();

        return chain.filter(exchange.mutate().request(enrichedRequest).build());
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator") ||
                path.startsWith("/health") ||
                path.startsWith("/fallback") ||
                path.startsWith("/api/docs") ||
                path.startsWith("/api/v1/chatbot");
    }

    private boolean isValidApiKey(String apiKey) {
        if (expectedApiKey == null || expectedApiKey.isEmpty()) {
            // If no API key configured, allow all (development mode)
            log.warn("No API key configured - allowing all requests (development mode)");
            return true;
        }
        return expectedApiKey.equals(apiKey);
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }
}
