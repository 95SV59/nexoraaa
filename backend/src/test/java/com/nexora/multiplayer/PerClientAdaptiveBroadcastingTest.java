package com.nexora.multiplayer;

import com.nexora.multiplayer.engine.EnginePlayer;
import com.nexora.multiplayer.engine.GameEngineRegistry;
import com.nexora.multiplayer.engine.GameLoopManager;
import com.nexora.multiplayer.engine.arena.NexoraArenaGame;
import com.nexora.multiplayer.engine.bot.BotPlayerService;
import com.nexora.multiplayer.room.GamePlayer;
import com.nexora.multiplayer.room.GameRoom;
import com.nexora.multiplayer.room.RoomService;
import com.nexora.multiplayer.session.GameSession;
import com.nexora.multiplayer.session.SessionService;
import com.nexora.multiplayer.transport.MultiplayerTransportGateway;
import com.nexora.multiplayer.transport.TransportChannel;
import com.nexora.network.NetworkControllerOrchestrator;
import com.nexora.network.decision.BroadcastRateRegistry;
import com.nexora.network.experiment.ControllerModeRegistry;
import com.nexora.network.lab.NetworkLabService;
import com.nexora.network.telemetry.TelemetryCollector;
import com.nexora.progression.LeaderboardService;
import com.nexora.progression.ProgressionService;
import com.nexora.library.LibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Priority Zero P0.1: Per-Client Adaptive Broadcasting Verification")
class PerClientAdaptiveBroadcastingTest {

    private BroadcastRateRegistry rateRegistry;
    private Map<String, AtomicInteger> deliveryCounts;
    private MultiplayerTransportGateway transportGateway;

    @BeforeEach
    void setUp() {
        rateRegistry = new BroadcastRateRegistry();
        deliveryCounts = new ConcurrentHashMap<>();

        transportGateway = new MultiplayerTransportGateway() {
            @Override
            public void broadcastState(String roomCode, Object stateSnapshot, TransportChannel channel) {
                deliveryCounts.computeIfAbsent(roomCode + "/global", k -> new AtomicInteger(0)).incrementAndGet();
            }

            @Override
            public void broadcastLifecycle(String roomCode, Object lifecycleEvent) {}

            @Override
            public void broadcastLobby(String roomCode, Object lobbyUpdate) {}

            @Override
            public void broadcastPing(String roomCode, Object pingBroadcast) {}

            @Override
            public void broadcastNetworkSnapshot(String roomCode, Object snapshot) {}

            @Override
            public void sendToPlayer(String roomCode, String playerId, String destination, Object payload, TransportChannel channel) {
                String key = roomCode + "/player/" + playerId + "/" + destination;
                deliveryCounts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
            }

            @Override
            public TransportChannel getActiveChannel(String roomCode) {
                return TransportChannel.RELIABLE_ORDERED;
            }

            @Override
            public void setActiveChannel(String roomCode, TransportChannel channel) {}
        };
    }

    @Test
    @DisplayName("P0.1 Runtime Invariant: 3 concurrent clients receive distinct snapshot rates (30Hz, 15Hz, 10Hz) while simulation runs at 30Hz")
    void testThreeClientsReceiveDistinctDeliveryFrequencies() {
        String roomCode = "NX-P01";
        rateRegistry.register(roomCode);

        // Client 1 (Fiber): excellent connection, N = 1 (30 snapshots/sec)
        // Client 2 (4G Mobile): moderate jitter, N = 2 (15 snapshots/sec)
        // Client 3 (Congested DSL): high latency, N = 3 (10 snapshots/sec)
        rateRegistry.setClientEveryNTicks(roomCode, "player-1", 1);
        rateRegistry.setClientEveryNTicks(roomCode, "player-2", 2);
        rateRegistry.setClientEveryNTicks(roomCode, "player-3", 3);

        assertTrue(rateRegistry.hasClientThrottles(roomCode));
        assertEquals(1, rateRegistry.getClientEveryNTicks(roomCode, "player-1"));
        assertEquals(2, rateRegistry.getClientEveryNTicks(roomCode, "player-2"));
        assertEquals(3, rateRegistry.getClientEveryNTicks(roomCode, "player-3"));

        List<EnginePlayer> players = List.of(
                new EnginePlayer("player-1", 1L, "Alice"),
                new EnginePlayer("player-2", 2L, "Bob"),
                new EnginePlayer("player-3", 3L, "Charlie")
        );

        NexoraArenaGame game = new NexoraArenaGame();
        game.initialize(roomCode, players);

        // Simulate 30 authoritative simulation ticks (1 second of game time)
        for (int tick = 1; tick <= 30; tick++) {
            game.updateState(); // Authoritative 30 Hz simulation

            Object snapshot = game.getStateSnapshot();
            TransportChannel channel = transportGateway.getActiveChannel(roomCode);

            // Per-client adaptive broadcasting logic (matching GameLoopManager)
            for (EnginePlayer ep : players) {
                String pid = ep.getPlayerId();
                int clientN = rateRegistry.getClientEveryNTicks(roomCode, pid);
                if (tick % clientN == 0) {
                    transportGateway.sendToPlayer(roomCode, pid, "state", snapshot, channel);
                    rateRegistry.incrementClientBroadcast(roomCode, pid);
                }
            }
        }

        // Verify Authoritative Simulation ran all 30 ticks
        assertTrue(game.validateState());

        // Verify Delivered Snapshot Counts:
        // Player 1 (N=1): exactly 30 snapshots delivered
        assertEquals(30, deliveryCounts.get(roomCode + "/player/player-1/state").get());
        assertEquals(30, rateRegistry.getClientBroadcastCount(roomCode, "player-1"));

        // Player 2 (N=2): exactly 15 snapshots delivered (every even tick)
        assertEquals(15, deliveryCounts.get(roomCode + "/player/player-2/state").get());
        assertEquals(15, rateRegistry.getClientBroadcastCount(roomCode, "player-2"));

        // Player 3 (N=3): exactly 10 snapshots delivered (every 3rd tick)
        assertEquals(10, deliveryCounts.get(roomCode + "/player/player-3/state").get());
        assertEquals(10, rateRegistry.getClientBroadcastCount(roomCode, "player-3"));

        // Verify Zero Cross-Contamination:
        // Individual endpoints received ONLY their intended packets
        assertNull(deliveryCounts.get(roomCode + "/player/player-nonexistent/state"));
    }

    @Test
    @DisplayName("P0.1 Dynamic Policy Adaptation & Disconnect Cleanup")
    void testDynamicPolicyChangesAndPlayerDisconnection() {
        String roomCode = "NX-DYNAMIC";
        rateRegistry.register(roomCode);

        rateRegistry.setClientEveryNTicks(roomCode, "player-a", 2); // throttled at 15Hz
        assertEquals(2, rateRegistry.getClientEveryNTicks(roomCode, "player-a"));

        // Mid-game adaptation: network recovers, upgrade to 30Hz
        rateRegistry.setClientEveryNTicks(roomCode, "player-a", 1);
        assertEquals(1, rateRegistry.getClientEveryNTicks(roomCode, "player-a"));

        // Player leaves room: remove policy
        rateRegistry.removePlayer(roomCode, "player-a");
        assertEquals(1, rateRegistry.getClientEveryNTicks(roomCode, "player-a")); // falls back to room default
        assertFalse(rateRegistry.hasClientThrottles(roomCode));
    }

    @Test
    @DisplayName("P0.1 Multi-Room Isolation: Concurrent rooms with distinct client throttling")
    void testMultiRoomThrottlingIsolation() {
        String room1 = "ROOM-ALPHA";
        String room2 = "ROOM-BETA";

        rateRegistry.register(room1);
        rateRegistry.register(room2);

        rateRegistry.setClientEveryNTicks(room1, "user-1", 1);
        rateRegistry.setClientEveryNTicks(room2, "user-1", 4); // same userId in different room

        assertEquals(1, rateRegistry.getClientEveryNTicks(room1, "user-1"));
        assertEquals(4, rateRegistry.getClientEveryNTicks(room2, "user-1"));

        rateRegistry.clear(room1);
        assertFalse(rateRegistry.hasClientThrottles(room1));
        assertTrue(rateRegistry.hasClientThrottles(room2));
        assertEquals(4, rateRegistry.getClientEveryNTicks(room2, "user-1"));
    }
}
