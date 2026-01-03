package com.banditgames.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API Gateway.
 * Provides aggregated API documentation for all backend services.
 */
@Configuration
public class OpenApiConfig {

    @Value("${GAME_SERVICE_URL:http://localhost:8082}")
    private String gameServiceUrl;

    @Value("${PLATFORM_BACKEND_URL:http://localhost:8081}")
    private String platformBackendUrl;

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BanditGames API Gateway")
                        .version("1.0.0")
                        .description("""
                                Centralized API Gateway for BanditGames Platform.

                                This gateway provides:
                                - Single entry point for all client requests
                                - Authentication and authorization via Keycloak
                                - Rate limiting and circuit breaking
                                - Request routing to backend services
                                - ACL (Anti-Corruption Layer) for external games
                                - Service-to-service authentication

                                **Backend Services:**
                                - **Game Service**: Game engine, AI player, ML models, chatbot, game logging
                                - **Platform Backend**: Lobbies, friends, achievements, game registry

                                **Authentication:**
                                All requests (except public endpoints) require a valid JWT token from Keycloak.
                                Include the token in the Authorization header: `Authorization: Bearer <token>`

                                **Rate Limiting:**
                                Different endpoints have different rate limits based on their usage patterns.

                                **Service-to-Service Communication:**
                                Internal services communicate using API keys via the `X-API-Key` header.
                                """)
                        .contact(new Contact()
                                .name("BanditGames Team")
                                .email("support@banditgames.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.banditgames.com")
                                .description("Production Server")));
    }
}
