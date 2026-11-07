package com.nexora.social.presence;

import com.nexora.security.AuthPrincipal;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketPresenceEventListener {

    private final PresenceService presenceService;

    public WebSocketPresenceEventListener(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal instanceof AuthPrincipal authPrincipal) {
            presenceService.updatePresence(authPrincipal.userId(), PresenceStatus.ONLINE, null, null);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal instanceof AuthPrincipal authPrincipal) {
            presenceService.updatePresence(authPrincipal.userId(), PresenceStatus.OFFLINE, null, null);
        }
    }
}
