package com.banditgames.platform.shared.security;

import com.banditgames.platform.shared.filter.GatewayUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility class for extracting information from Auth.
 */
public class AuthUtils {

    /**
     * Extracts the user ID (subject) from the current authentication.
     *
     * @return The user ID from the Authentication as UUID
     * @throws IllegalStateException if no authentication is present or token is invalid
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        GatewayUserPrincipal principal = (GatewayUserPrincipal) authentication.getPrincipal();

        if (principal == null || principal.getUserId().toString().isEmpty()) {
            throw new IllegalStateException("Unable to extract user ID from authentication");
        }

        try {
            return principal.getUserId();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("User ID from token is not a valid UUID: " + principal.getUserId(), e);
        }

    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }
}

