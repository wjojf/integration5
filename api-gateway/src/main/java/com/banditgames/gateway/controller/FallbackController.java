package com.banditgames.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback controller for circuit breaker scenarios.
 * Returns appropriate error responses when backend services are unavailable.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/platform")
    public Mono<ResponseEntity<Map<String, Object>>> platformFallback() {
        log.warn("Platform backend service unavailable - returning fallback response");
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("platform-backend", "Service temporarily unavailable")));
    }

    @GetMapping("/game")
    public Mono<ResponseEntity<Map<String, Object>>> gameServiceFallback() {
        log.warn("Game service unavailable - returning fallback response");
        return Mono.just(ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(createFallbackResponse("game-service", "Service temporarily unavailable")));
    }

    private Map<String, Object> createFallbackResponse(String service, String message) {
        return Map.of(
            "error", "Service Unavailable",
            "service", service,
            "message", message,
            "timestamp", Instant.now().toString(),
            "retryAfter", 30
        );
    }
}
