package com.banditgames.platform.shared.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
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

        // Add ROLE_USER authority so @PreAuthorize("isAuthenticated()") works
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Also add any roles from X-User-Roles header if present
        String rolesHeader = req.getHeader("X-User-Roles");
        if (rolesHeader != null && !rolesHeader.isEmpty()) {
            String[] roles = rolesHeader.split(",");
            for (String role : roles) {
                String trimmedRole = role.trim();
                if (!trimmedRole.isEmpty()) {
                    // Ensure role has ROLE_ prefix
                    String roleWithPrefix = trimmedRole.startsWith("ROLE_") 
                        ? trimmedRole 
                        : "ROLE_" + trimmedRole;
                    authorities.add(new SimpleGrantedAuthority(roleWithPrefix));
                }
            }
        }

        PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(principal, "N/A", authorities);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(req, res);
    }
}

