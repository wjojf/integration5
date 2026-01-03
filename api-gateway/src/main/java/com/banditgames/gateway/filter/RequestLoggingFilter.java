package com.banditgames.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Request logging filter for monitoring and debugging.
 * Logs request details and response times.
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final int FILTER_ORDER = 1000;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Instant startTime = Instant.now();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String userId = request.getHeaders().getFirst("X-User-Id");
        String serviceName = request.getHeaders().getFirst("X-Service-Name");

        return chain.filter(exchange)
            .doOnSuccess(aVoid -> {
                Duration duration = Duration.between(startTime, Instant.now());
                int statusCode = exchange.getResponse().getStatusCode() != null 
                    ? exchange.getResponse().getStatusCode().value() 
                    : 0;
                
                log.info("Request completed: method={}, path={}, status={}, duration={}ms, userId={}, service={}",
                    method, path, statusCode, duration.toMillis(), userId, serviceName);
            })
            .doOnError(throwable -> {
                Duration duration = Duration.between(startTime, Instant.now());
                log.error("Request failed: method={}, path={}, duration={}ms, error={}",
                    method, path, duration.toMillis(), throwable.getMessage());
            });
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }
}
