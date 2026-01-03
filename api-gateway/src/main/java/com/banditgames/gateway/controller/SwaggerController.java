package com.banditgames.gateway.controller;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller to expose OpenAPI specification for API Gateway.
 * Spring Cloud Gateway doesn't automatically expose OpenAPI docs,
 * so we need this controller to serve the spec.
 * 
 * Note: For aggregated docs, use GatewayDocumentationController.
 */
@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class SwaggerController {

    private final OpenAPI openAPI;

    /**
     * Get gateway-only OpenAPI specification.
     * For aggregated docs from all services, use /api/docs/openapi.json
     */
    @GetMapping("/gateway-openapi.json")
    public Mono<ResponseEntity<OpenAPI>> getGatewayOpenApiSpec() {
        return Mono.just(ResponseEntity.ok(openAPI));
    }
}
