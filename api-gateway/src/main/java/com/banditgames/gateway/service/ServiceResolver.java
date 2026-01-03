package com.banditgames.gateway.service;

import com.banditgames.gateway.config.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

/**
 * Service resolver utility.
 * Determines service names from exchange context (route, path, etc.).
 * 
 * This provides a single point of service name resolution that can be used
 * across all filters, making the codebase more maintainable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceResolver {

    private final ServiceRegistry serviceRegistry;

    /**
     * Resolve service name from exchange.
     * Tries multiple strategies in order:
     * 1. Route ID (exact match)
     * 2. Route ID (prefix match)
     * 3. Request path (prefix match)
     * 
     * @param exchange The server web exchange
     * @return Service name, or "unknown" if cannot be determined
     */
    @SuppressWarnings("null")
    public String resolveServiceName(ServerWebExchange exchange) {
        // Strategy 1: Try route ID
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null && route.getId() != null) {
            Optional<String> serviceName = serviceRegistry.resolveFromRouteId(route.getId());
            if (serviceName.isPresent()) {
                log.debug("Resolved service name from route ID: {} -> {}", route.getId(), serviceName.get());
                return serviceName.get();
            }
        }

        // Strategy 2: Try request path
        String path = exchange.getRequest().getURI().getPath();
        Optional<String> serviceName = serviceRegistry.resolveFromPath(path);
        if (serviceName.isPresent()) {
            log.debug("Resolved service name from path: {} -> {}", path, serviceName.get());
            return serviceName.get();
        }

        // Strategy 3: Fallback to route ID if available
        if (route != null && route.getId() != null) {
            log.debug("Using route ID as service name: {}", route.getId());
            return route.getId();
        }

        log.warn("Could not resolve service name for path: {}", path);
        return "unknown";
    }
}
