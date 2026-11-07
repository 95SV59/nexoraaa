package com.nexora.network.decision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PWNC Stage 207: Broadcast Rate Registry Unit Tests")
class BroadcastRateRegistryTest {

    private BroadcastRateRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BroadcastRateRegistry();
    }

    @Test
    @DisplayName("Defaults to 1 tick per broadcast on registration")
    void testDefaultRateIsOne() {
        registry.register("ROOM-101");
        assertEquals(1, registry.getEveryNTicks("ROOM-101"));
        assertEquals(1, registry.getClientEveryNTicks("ROOM-101", "player-1"));
    }

    @Test
    @DisplayName("Sets and retrieves room-level broadcast throttle")
    void testRoomLevelThrottle() {
        registry.register("ROOM-202");
        registry.setEveryNTicks("ROOM-202", 2);
        assertEquals(2, registry.getEveryNTicks("ROOM-202"));

        // Clients without explicit override inherit room-level rate
        assertEquals(2, registry.getClientEveryNTicks("ROOM-202", "player-1"));
        assertEquals(2, registry.getClientEveryNTicks("ROOM-202", "player-2"));
    }

    @Test
    @DisplayName("Supports per-client override for congested players")
    void testPerClientThrottleOverride() {
        registry.register("ROOM-303");
        registry.setEveryNTicks("ROOM-303", 1); // room default is 1 (30 Hz)

        // Congested mobile player gets throttled to every 3 ticks (10 Hz)
        registry.setClientEveryNTicks("ROOM-303", "player-congested", 3);

        assertEquals(1, registry.getClientEveryNTicks("ROOM-303", "player-fiber"));
        assertEquals(3, registry.getClientEveryNTicks("ROOM-303", "player-congested"));

        Map<String, Integer> throttles = registry.getAllClientThrottles("ROOM-303");
        assertTrue(throttles.containsKey("player-congested"));
        assertEquals(3, throttles.get("player-congested"));

        // Clear specific client
        registry.clearClient("ROOM-303", "player-congested");
        assertEquals(1, registry.getClientEveryNTicks("ROOM-303", "player-congested"));
    }

    @Test
    @DisplayName("Clear removes both room and client throttles")
    void testClearRemovesAll() {
        registry.register("ROOM-404");
        registry.setEveryNTicks("ROOM-404", 2);
        registry.setClientEveryNTicks("ROOM-404", "p1", 3);

        registry.clear("ROOM-404");
        assertEquals(1, registry.getEveryNTicks("ROOM-404")); // fallback to 1
        assertTrue(registry.getAllClientThrottles("ROOM-404").isEmpty());
    }
}
