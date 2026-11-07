package com.nexora.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * The WebSocket half of authentication (see SecurityConfig's javadoc for why REST and
 * WebSocket are checked in two different places). A browser's WebSocket upgrade request
 * can't carry a custom Authorization header, but @stomp/stompjs's `connectHeaders` option
 * sends one inside the STOMP CONNECT frame itself, over the already-established transport —
 * that's what this reads.
 *
 * A missing or invalid token throws rather than lets the CONNECT through with no user:
 * Spring converts the exception into a STOMP ERROR frame and closes the session, so a bad
 * token fails loudly here instead of surfacing later as a confusing NullPointerException
 * the first time some @MessageMapping handler calls principal.getName().
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null) {
                header = accessor.getFirstNativeHeader("authorization");
            }
            AuthPrincipal principal = null;
            if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = header.substring(7).trim();
                if (!token.isEmpty()) {
                    principal = jwtService.validateAndGetPrincipal(token).orElse(null);
                }
            }
            if (principal == null) {
                throw new IllegalArgumentException("Missing or invalid Authorization token on STOMP CONNECT");
            }
            accessor.setUser(principal);
        }
        return message;
    }
}
