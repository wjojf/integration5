package com.banditgames.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

/**
 * Rate limiting configuration.
 * Resolves rate limit keys based on authenticated user ID or IP address.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
            .map(securityContext -> {
                Authentication authentication = securityContext.getAuthentication();
                if (isJwtAuthentication(authentication)) {
                    Jwt jwt = (Jwt) authentication.getPrincipal();
                    return extractUserId(jwt);
                }
                return extractIpAddress(exchange);
            })
            .switchIfEmpty(Mono.just("anonymous"));
    }

    private boolean isJwtAuthentication(Authentication authentication) {
        return authentication != null
            && authentication.getPrincipal() instanceof Jwt;
    }

    private String extractUserId(Jwt jwt) {
        return Optional.ofNullable(jwt.getClaimAsString(GatewayConstants.JWT_CLAIM_SUB))
            .orElse("unknown");
    }

    private String extractIpAddress(org.springframework.web.server.ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getAddress).map(InetAddress::toString).orElse("unknown");
    }
}


