package com.banditgames.platform.shared.config;

import org.springframework.context.annotation.Configuration;

/**
 * WebSocket security configuration.
 * 
 * Authentication is handled manually by the custom JWT channel interceptor
 * in WebSocketAuthenticationConfig. This config just ensures Spring Security's
 * WebSocket interceptors don't interfere.
 * 
 * Note: HTTP security for /ws/** is configured in SecurityConfig (permitAll).
 * CSRF is already disabled globally.
 */
@Configuration
public class WebSocketSecurityConfig {
    // No @EnableWebSocketSecurity - we handle JWT auth manually in WebSocketAuthenticationConfig
    // The SecurityConfig already permits /ws/** and disables CSRF
}
