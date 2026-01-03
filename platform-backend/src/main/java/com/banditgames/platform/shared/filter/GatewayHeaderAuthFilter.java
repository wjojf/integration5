package com.banditgames.platform.shared.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Skip filter for public endpoints to avoid authentication issues
        String path = req.getRequestURI();
        if (path.startsWith("/api/external/") || 
            path.startsWith("/actuator/") || 
            path.startsWith("/swagger-ui/") || 
            path.startsWith("/v3/api-docs/") ||
            path.startsWith("/error")) {
            chain.doFilter(req, res);
            return;
        }

        String stringUserId = req.getHeader("X-User-Id");
        if (stringUserId == null || stringUserId.isEmpty()) {
            // For authenticated endpoints without header, clear context
            SecurityContextHolder.clearContext();
            chain.doFilter(req, res);
            return;
        }

        UUID userId = UUID.fromString(stringUserId);
        String username = req.getHeader("X-Username");
        String email = req.getHeader("X-User-Email");

        GatewayUserPrincipal principal = new GatewayUserPrincipal(userId, username, email);

        PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(principal, "N/A", List.of());
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(req, res);
    }
}

