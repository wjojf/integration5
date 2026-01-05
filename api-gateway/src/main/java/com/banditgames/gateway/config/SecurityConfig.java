package com.banditgames.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for API Gateway.
 * Configures JWT validation with Keycloak and public endpoints.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkPath;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(
                    GatewayConstants.PATH_ACTUATOR,
                    GatewayConstants.PATH_HEALTH,
                    GatewayConstants.PATH_FALLBACK,
                    GatewayConstants.PATH_GAMES,
                    "/api/docs/**",  // Allow Swagger UI access
                    "/api/platform/register/**",  // Allow chess game registration (external service)
                    "/api/external/**",  // Allow all external game endpoints
                    "/api/v1/chatbot/**"  // Allow chatbot endpoints (no rate limiting or circuit breaker)
                ).permitAll()
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .pathMatchers(GatewayConstants.PATH_PATTERN_EXTERNAL).permitAll()
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwkSetUri(jwkPath))
            )
            .build();
    }
}


