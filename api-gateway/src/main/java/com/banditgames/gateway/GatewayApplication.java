package com.banditgames.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * API Gateway Application
 * 
 * Centralized entry point for all client requests to the BanditGames platform.
 * 
 * Features:
 * - JWT authentication via Keycloak
 * - Request routing to backend services
 * - Rate limiting and circuit breaking
 * - ACL for external game integration
 * - Service-to-service authentication
 * - Swagger/OpenAPI documentation
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}


