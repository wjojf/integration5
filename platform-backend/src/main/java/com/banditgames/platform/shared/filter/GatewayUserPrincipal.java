package com.banditgames.platform.shared.filter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class GatewayUserPrincipal {
    private final UUID userId;
    private final String username;
    private final String email;
}
