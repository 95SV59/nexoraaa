package com.nexora.network.lab;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Phase 16 — Network Laboratory. Deliberately an APPLICATION-LAYER simulation at the
 * WebSocket message boundary (delaying/dropping messages in GameWebSocketController)
 * rather than OS-level traffic shaping (tc/netem) — this runs anywhere the backend
 * runs, needs no elevated privileges, and is controllable per-room from the browser,
 * which matters more for a course demo than kernel-level fidelity. See
 * network-lab/README.md for the full rationale and config shape.
 *
 * Applying an active profile's delay to the server-timed ping/pong round trip (see
 * GameWebSocketController.handlePong) means the artificial latency shows up as real,
 * measured RTT to TelemetryCollector — the predictor and controller downstream react
 * to it exactly as they would to a genuinely bad connection.
 */
@Component
public class NetworkLabService {

    private final Map<String, NetworkLabProfile> activeProfiles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Random random = new Random();

    public void setProfile(String roomCode, NetworkLabProfile profile) {
        if (profile == null || profile.isOff()) {
            activeProfiles.remove(roomCode);
        } else {
            activeProfiles.put(roomCode, profile);
        }
    }

    public void clearProfile(String roomCode) {
        activeProfiles.remove(roomCode);
    }

    public NetworkLabProfile getProfile(String roomCode) {
        return activeProfiles.getOrDefault(roomCode, NetworkLabProfile.off());
    }

    /** True if a simulated packet for this room should be treated as lost. */
    public boolean shouldDrop(String roomCode) {
        NetworkLabProfile p = activeProfiles.get(roomCode);
        if (p == null || p.packetLossPct() <= 0) return false;
        return random.nextDouble() * 100 < p.packetLossPct();
    }

    /** Runs action immediately if no profile is active, otherwise after
     *  latencyMs +/- a random jitterMs offset — see class javadoc. */
    public void runWithSimulatedDelay(String roomCode, Runnable action) {
        NetworkLabProfile p = activeProfiles.get(roomCode);
        if (p == null || (p.latencyMs() <= 0 && p.jitterMs() <= 0)) {
            action.run();
            return;
        }
        double jitterOffset = p.jitterMs() <= 0 ? 0 : (random.nextDouble() * 2 - 1) * p.jitterMs();
        long delayMs = Math.max(0, Math.round(p.latencyMs() + jitterOffset));
        if (delayMs == 0) {
            action.run();
        } else {
            executor.schedule(action, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
