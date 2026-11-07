package com.nexora.config;

import com.nexora.security.StompAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * STOMP-over-WebSocket wiring (Phase 7, secured in Phase 5). Clients connect to /ws,
 * authenticate on the STOMP CONNECT frame (see StompAuthChannelInterceptor), subscribe to
 * /topic/room/{roomCode}/{lobby|state|lifecycle|ping|network}, and send to
 * /app/room/{roomCode}/... See docs/WEBSOCKET_PROTOCOL.md for the message catalog.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // server -> client broadcasts
        registry.setApplicationDestinationPrefixes("/app"); // client -> server
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
