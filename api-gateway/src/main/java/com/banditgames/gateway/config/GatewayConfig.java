package com.banditgames.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * Gateway routing configuration.
 * Defines routes to backend services with filters for rate limiting and circuit breaking.
 * Note: Rate limiting configuration is done via application.yml for simplicity.
 * This class can be extended if programmatic route configuration is needed.
 * 
 * Service registry is automatically initialized via @PostConstruct in ServiceRegistry.
 */
@Configuration
public class GatewayConfig {
    // Routes are configured in application.yml
    // Service registry is configured via @ConfigurationProperties in ServiceRegistry
}

