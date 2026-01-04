package com.banditgames.platform.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Custom JWT configuration to handle Docker networking.
 * 
 * Only validates JWT signature using the JWK set (no issuer validation).
 * This matches the api-gateway approach and avoids issuer mismatch between:
 * - Frontend accessing Keycloak via localhost:8180
 * - Backend accessing Keycloak via keycloak:8080 (Docker internal)
 */
@Configuration
public class JwtConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        // Only validate signature using JWK set, skip issuer validation
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}

