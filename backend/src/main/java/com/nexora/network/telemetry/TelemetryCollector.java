package com.nexora.network.telemetry;

import com.nexora.multiplayer.room.RoomService;
import com.nexora.multiplayer.websocket.dto.PingBroadcast;
import com.nexora.network.persistence.NetworkPersistenceService;
import jakarta.annotation.PreDestroy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 10 — Network Telemetry. Measures RTT/jitter via a server-initiated ping/pong
 * (server time only, so no client clock is ever trusted for a number the controller
 * acts on), packet loss via ping/pong delivery rate, and throughput via received
 * PLAYER_INPUT volume. Produces one aggregated NetworkSample per room per window,
 * which is the sole input to network.prediction.NetworkPredictor, and persists each
 * sample (Phase 10 -> NetworkPersistenceService) against the match's GameSession.
 */
@Component
public class TelemetryCollector {

    private static final long PING_INTERVAL_MS = 2000;
    private static final long AGGREGATE_DELAY_MS = 500; // time given for pongs to arrive before aggregating
    private static final int MAX_SAMPLES_PER_ROOM = 40; // ~80s of history at the interval above

    private static class PlayerTelemetryState {
        volatile double lastRtt = -1;
        volatile double lastJitter = 0;
        final AtomicLong inputCountThisWindow = new AtomicLong(0);
    }

    private static class RoomTelemetryState {
        final Map<String, PlayerTelemetryState> players = new ConcurrentHashMap<>();
        final Deque<NetworkSample> samples = new ArrayDeque<>();
        final AtomicLong pingIdSeq = new AtomicLong(1);
        volatile long currentPingId = -1;
        volatile int pingsSentThisWindow = 0;
        final AtomicLong pongsReceivedThisWindow = new AtomicLong(0);
        Long sessionId;
        ScheduledFuture<?> pingTask;
    }

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Map<String, RoomTelemetryState> rooms = new ConcurrentHashMap<>();

    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NetworkPersistenceService persistenceService;

    public TelemetryCollector(RoomService roomService, SimpMessagingTemplate messagingTemplate,
                               NetworkPersistenceService persistenceService) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
        this.persistenceService = persistenceService;
    }

    public record ClientTelemetrySnapshot(String playerId, double rtt, double jitter, double throughput) {}

    public void startTracking(String roomCode, Long sessionId) {
        rooms.computeIfAbsent(roomCode, code -> {
            RoomTelemetryState state = new RoomTelemetryState();
            state.sessionId = sessionId;
            state.pingTask = executor.scheduleAtFixedRate(
                    () -> sendPing(code, state), 0, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
            return state;
        });
    }

    public void stopTracking(String roomCode) {
        RoomTelemetryState state = rooms.remove(roomCode);
        if (state != null && state.pingTask != null) {
            state.pingTask.cancel(false);
        }
    }

    public int getTrackedRoomCount() {
        return rooms.size();
    }

    public void onPongReceived(String roomCode, String playerId, long pingId, long serverSendTime) {
        if (playerId != null && playerId.startsWith("bot-")) return; // Bot players must never contaminate network measurements
        RoomTelemetryState room = rooms.get(roomCode);
        if (room == null || pingId != room.currentPingId) return; // stale/late pong, ignore

        double rtt = System.currentTimeMillis() - serverSendTime;
        PlayerTelemetryState p = room.players.computeIfAbsent(playerId, id -> new PlayerTelemetryState());
        double jitter = p.lastRtt >= 0 ? Math.abs(rtt - p.lastRtt) : 0;
        p.lastRtt = rtt;
        p.lastJitter = jitter;
        room.pongsReceivedThisWindow.incrementAndGet();
    }

    public void onInputReceived(String roomCode, String playerId) {
        if (playerId != null && playerId.startsWith("bot-")) return;
        RoomTelemetryState room = rooms.get(roomCode);
        if (room == null) return;
        room.players.computeIfAbsent(playerId, id -> new PlayerTelemetryState())
                .inputCountThisWindow.incrementAndGet();
    }

    /** Returns per-client telemetry snapshots for the room, excluding bots. */
    public Map<String, ClientTelemetrySnapshot> getClientSnapshots(String roomCode) {
        RoomTelemetryState room = rooms.get(roomCode);
        if (room == null) return Map.of();
        Map<String, ClientTelemetrySnapshot> result = new ConcurrentHashMap<>();
        double windowSeconds = (PING_INTERVAL_MS) / 1000.0;
        for (Map.Entry<String, PlayerTelemetryState> entry : room.players.entrySet()) {
            String pid = entry.getKey();
            if (pid != null && pid.startsWith("bot-")) continue;
            PlayerTelemetryState p = entry.getValue();
            if (p.lastRtt >= 0) {
                result.put(pid, new ClientTelemetrySnapshot(pid, p.lastRtt, p.lastJitter, p.inputCountThisWindow.get() / windowSeconds));
            }
        }
        return result;
    }

    /** Most recent N samples, oldest first — what the prediction engine consumes. */
    public List<NetworkSample> getRecentSamples(String roomCode, int n) {
        RoomTelemetryState room = rooms.get(roomCode);
        if (room == null) return List.of();
        synchronized (room.samples) {
            return room.samples.stream().skip(Math.max(0, room.samples.size() - n)).toList();
        }
    }

    public NetworkSample getLatestSample(String roomCode) {
        RoomTelemetryState room = rooms.get(roomCode);
        if (room == null) return null;
        synchronized (room.samples) {
            return room.samples.peekLast();
        }
    }

    private void sendPing(String roomCode, RoomTelemetryState room) {
        int activePlayerCount = (int) roomService.getActivePlayers(
                roomService.getRoomOrThrow(roomCode).getId()).stream()
                .filter(p -> !p.isBot() && !p.isSpectator())
                .count();
        if (activePlayerCount == 0) return;

        long pingId = room.pingIdSeq.getAndIncrement();
        room.currentPingId = pingId;
        room.pingsSentThisWindow = activePlayerCount;
        room.pongsReceivedThisWindow.set(0);

        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/ping",
                new PingBroadcast(pingId, System.currentTimeMillis()));

        executor.schedule(() -> aggregate(roomCode, room), AGGREGATE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void aggregate(String roomCode, RoomTelemetryState room) {
        if (room.players.isEmpty()) return;

        double maxRtt = 0, maxJitter = 0;
        double throughputSum = 0;
        double windowSeconds = (PING_INTERVAL_MS) / 1000.0;
        for (PlayerTelemetryState p : room.players.values()) {
            if (p.lastRtt >= 0) {
                maxRtt = Math.max(maxRtt, p.lastRtt);
                maxJitter = Math.max(maxJitter, p.lastJitter);
            }
            throughputSum += p.inputCountThisWindow.getAndSet(0) / windowSeconds;
        }

        double lossPct = room.pingsSentThisWindow == 0 ? 0
                : 100.0 * (1.0 - (double) room.pongsReceivedThisWindow.get() / room.pingsSentThisWindow);
        lossPct = Math.max(0, Math.min(100, lossPct));

        NetworkSample sample = new NetworkSample(System.currentTimeMillis(), maxRtt, maxJitter, lossPct, throughputSum);
        synchronized (room.samples) {
            room.samples.addLast(sample);
            while (room.samples.size() > MAX_SAMPLES_PER_ROOM) {
                room.samples.removeFirst();
            }
        }
        if (room.sessionId != null) {
            persistenceService.recordTelemetry(room.sessionId, sample);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
