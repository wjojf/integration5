package com.banditgames.platform.shared.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketAuthenticationConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");

                    if (authorization != null && !authorization.isEmpty()) {
                        String bearerToken = authorization.getFirst();

                        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                            String token = bearerToken.substring(7);

                            try {
                                Jwt jwt = jwtDecoder.decode(token);
                                String userId = jwt.getSubject();
                                String username = jwt.getClaimAsString("preferred_username");
                                
                                log.info("WebSocket CONNECT: Validating JWT for user={}, sub={}", username, userId);

                                // Provide ROLE_USER authority - required for message authorization
                                var authorities = Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_USER")
                                );

                                UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                                
                                accessor.setUser(authentication);
                                SecurityContextHolder.getContext().setAuthentication(authentication);

                                log.info("WebSocket connection authenticated for user: {} ({})", username, userId);
                            } catch (Exception e) {
                                log.error("Failed to authenticate WebSocket connection: {}", e.getMessage(), e);
                                throw new IllegalArgumentException("Invalid JWT token: " + e.getMessage());
                            }
                        } else {
                            log.warn("WebSocket connection attempt with invalid Authorization header format");
                            throw new IllegalArgumentException("Invalid Authorization header format");
                        }
                    } else {
                        log.warn("WebSocket connection attempt without Authorization header");
                        throw new IllegalArgumentException("Missing Authorization header");
                    }
                }

                return message;
            }
        });
    }
}

