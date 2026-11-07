package com.nexora.security;

import java.security.Principal;

/**
 * The identity object both REST (@AuthenticationPrincipal) and WebSocket (STOMP Principal)
 * code deals with post-authentication.
 */
public record AuthPrincipal(Long userId, String username, String role) implements Principal {

    public AuthPrincipal(Long userId, String username) {
        this(userId, username, "PLAYER");
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
