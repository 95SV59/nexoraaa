package com.nexora.multiplayer.transport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Multiplayer Transport Gateway Unit Tests")
class MultiplayerTransportGatewayTest {

    private SimpMessagingTemplate messagingTemplate;
    private StompWebSocketTransportGateway gateway;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        gateway = new StompWebSocketTransportGateway(messagingTemplate);
    }

    @Test
    @DisplayName("Defaults to RELIABLE_ORDERED channel and allows dynamic switching")
    void testChannelDefaultAndSwitching() {
        String room = "ROOM-ABC";
        assertEquals(TransportChannel.RELIABLE_ORDERED, gateway.getActiveChannel(room));

        gateway.setActiveChannel(room, TransportChannel.UNRELIABLE_UNORDERED);
        assertEquals(TransportChannel.UNRELIABLE_UNORDERED, gateway.getActiveChannel(room));

        gateway.setActiveChannel(room, null);
        assertEquals(TransportChannel.RELIABLE_ORDERED, gateway.getActiveChannel(room));
    }

    @Test
    @DisplayName("Broadcasts state to correct STOMP destination")
    void testBroadcastState() {
        String room = "ROOM-XYZ";
        Object dummyState = "STATE_SNAPSHOT";

        gateway.broadcastState(room, dummyState, TransportChannel.RELIABLE_ORDERED);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/room/" + room + "/state", dummyState);
    }

    @Test
    @DisplayName("Broadcasts lifecycle and lobby events reliably")
    void testBroadcastLifecycleAndLobby() {
        String room = "ROOM-LIFE";
        Object lifecycleMsg = "MATCH_START";
        Object lobbyMsg = "LOBBY_UPDATE";

        gateway.broadcastLifecycle(room, lifecycleMsg);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/room/" + room + "/lifecycle", lifecycleMsg);

        gateway.broadcastLobby(room, lobbyMsg);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/room/" + room + "/lobby", lobbyMsg);
    }

    @Test
    @DisplayName("Tracks message sequencing and channel counters across reliable and unreliable channels")
    void testMessageSequencingAndChannelCounters() {
        String room = "ROOM-METRICS";
        assertEquals(0, gateway.getLastSequence(room));

        // Send reliable state
        gateway.broadcastState(room, "SNAP-1", TransportChannel.RELIABLE_ORDERED);
        assertEquals(1, gateway.getLastSequence(room));
        assertEquals(1, gateway.getReliableCount(room));
        assertEquals(0, gateway.getUnreliableCount(room));

        // Send unreliable state
        gateway.broadcastState(room, "SNAP-2", TransportChannel.UNRELIABLE_UNORDERED);
        assertEquals(2, gateway.getLastSequence(room));
        assertEquals(1, gateway.getReliableCount(room));
        assertEquals(1, gateway.getUnreliableCount(room));

        // Send to player on unreliable channel
        gateway.sendToPlayer(room, "p1", "state", "SNAP-3", TransportChannel.UNRELIABLE_UNORDERED);
        assertEquals(3, gateway.getLastSequence(room));
        assertEquals(2, gateway.getUnreliableCount(room));
        verify(messagingTemplate, times(1)).convertAndSend("/topic/room/" + room + "/player/p1/state", "SNAP-3");
    }
}
