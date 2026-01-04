package com.banditgames.platform.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

import static org.springframework.messaging.simp.SimpMessageType.*;

/**
 * WebSocket message security configuration.
 * 
 * In Spring Security 6+, WebSocket message security is configured separately from HTTP security.
 * This configuration:
 * 1. Enables WebSocket security (automatically disables CSRF for SockJS)
 * 2. Permits CONNECT frames (nullDestMatcher) which is essential for STOMP connections
 * 3. Allows all messages since authentication is handled by the custom channel interceptor
 *    in WebSocketAuthenticationConfig
 * 
 * IMPORTANT: The @EnableWebSocketSecurity annotation automatically disables CSRF for
 * SockJS WebSocket connections, which is essential for STOMP clients that send
 * JWT tokens in the connect headers instead of CSRF tokens.
 */
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    /**
     * Configure message security - permit all messages since authentication
     * is handled by the custom JWT channel interceptor.
     * 
     * The custom interceptor in WebSocketAuthenticationConfig validates JWT tokens
     * on CONNECT and sets up the user principal.
     * 
     * nullDestMatcher().permitAll() is CRITICAL - it allows STOMP CONNECT frames
     * which don't have a destination.
     */
    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
            // Allow CONNECT, DISCONNECT, HEARTBEAT, and other control messages
            .nullDestMatcher().permitAll()
            // Allow all SUBSCRIBE messages (destinations checked by application logic)
            .simpTypeMatchers(SUBSCRIBE).permitAll()
            // Allow all MESSAGE types (for publishing)
            .simpTypeMatchers(MESSAGE).permitAll()
            // Allow all DISCONNECT
            .simpTypeMatchers(DISCONNECT).permitAll()
            // Allow all application destinations
            .simpDestMatchers("/app/**").permitAll()
            // Allow all subscription destinations
            .simpSubscribeDestMatchers("/topic/**", "/queue/**", "/user/**").permitAll()
            // Permit any other messages (fallback)
            .anyMessage().permitAll();
        
        return messages.build();
    }
}
