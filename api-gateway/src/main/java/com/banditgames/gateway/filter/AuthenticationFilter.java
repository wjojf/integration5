package com.banditgames.gateway.filter;

import com.banditgames.gateway.config.GatewayConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Global filter that extracts user information from JWT and adds it to request
 * headers.
 * This allows downstream services to access user context without validating JWT
 * again.
 */
@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final int FILTER_ORDER = -100;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> {
                    Authentication authentication = securityContext.getAuthentication();
                    if (isJwtAuthentication(authentication)) {
                        return enrichRequestWithUserInfo(exchange, (Jwt) authentication.getPrincipal());
                    }
                    return exchange;
                })
                .switchIfEmpty(Mono.just(exchange))
                .flatMap(chain::filter);
    }

    private boolean isJwtAuthentication(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof Jwt;
    }

    private ServerWebExchange enrichRequestWithUserInfo(
            ServerWebExchange exchange,
            Jwt jwt) {

        String userId = extractClaim(jwt, GatewayConstants.JWT_CLAIM_SUB, "unknown");
        String username = extractClaim(jwt, GatewayConstants.JWT_CLAIM_PREFERRED_USERNAME, "");
        String email = extractClaim(jwt, GatewayConstants.JWT_CLAIM_EMAIL, "");
        List<String> roles = extractRoles(jwt);

        ServerHttpRequest enrichedRequest = exchange.getRequest().mutate()
                .header(GatewayConstants.HEADER_USER_ID, userId)
                .header(GatewayConstants.HEADER_USERNAME, username)
                .header(GatewayConstants.HEADER_USER_EMAIL, email)
                .header(GatewayConstants.HEADER_USER_ROLES, String.join(",", roles))
                .build();

        log.debug("Enriched request with user info: userId={}, username={}", userId, username);
        return exchange.mutate().request(enrichedRequest).build();
    }

    private String extractClaim(Jwt jwt, String claimName, String defaultValue) {
        return Optional.ofNullable(jwt.getClaimAsString(claimName))
                .orElse(defaultValue);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map) {
                Object roles = ((java.util.Map<String, Object>) realmAccess).get("roles");
                if (roles instanceof List) {
                    return (List<String>) roles;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract roles from JWT", e);
        }
        return Collections.emptyList();
    }

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

}
