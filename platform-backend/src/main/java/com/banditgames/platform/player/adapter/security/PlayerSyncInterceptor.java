package com.banditgames.platform.player.adapter.security;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.Rank;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import com.banditgames.platform.player.port.out.SavePlayerPort;
import com.banditgames.platform.shared.filter.GatewayUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerSyncInterceptor implements HandlerInterceptor {

    private final LoadPlayerPort loadPlayerPort;
    private final SavePlayerPort savePlayerPort;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Skip for public endpoints or if authentication is not available
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof GatewayUserPrincipal)) {
            return true;
        }

        GatewayUserPrincipal principal = (GatewayUserPrincipal) authentication.getPrincipal();

        if (loadPlayerPort.findById(principal.getUserId()).isEmpty()) {
            String email = principal.getEmail();
            String username = principal.getUsername();

            Player newPlayer = Player.builder()
                    .playerId(principal.getUserId())
                    .username(username)
                    .email(email)
                    .bio("")
                    .address(null)
                    .rank(Rank.BRONZE)
                    .exp(0)
                    .gamePreferences(new ArrayList<>())
                    .build();

            savePlayerPort.save(newPlayer);

            log.info("Auto-created player profile for user: {} with username: {}", principal.getUserId(), username);
                    }


        return true;
    }
}

