package com.nexora.network.decision;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 14 & Patent Stage 207 (Live Network Controller):
 * Manages both room-wide and per-client broadcast rate throttles.
 * Read by GameLoopManager every tick, updated by NetworkControllerOrchestrator
 * on every decision cycle.
 *
 * Simulation itself (MultiplayerGame.updateState()) always runs at the game's full tick
 * rate (30 Hz) regardless of this setting — only outbound message broadcasts are throttled,
 * reducing network load without ever letting server-side physics fall behind or compound error.
 */
@Component
public class BroadcastRateRegistry {

    private final Map<String, Integer> roomEveryNTicks = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> clientEveryNTicks = new ConcurrentHashMap<>();
    private final Map<String, Map<String, java.util.concurrent.atomic.AtomicLong>> clientBroadcastCounters = new ConcurrentHashMap<>();

    public void register(String roomCode) {
        roomEveryNTicks.put(roomCode, 1);
        clientEveryNTicks.put(roomCode, new ConcurrentHashMap<>());
        clientBroadcastCounters.put(roomCode, new ConcurrentHashMap<>());
    }

    public void clear(String roomCode) {
        roomEveryNTicks.remove(roomCode);
        clientEveryNTicks.remove(roomCode);
        clientBroadcastCounters.remove(roomCode);
    }

    public void setEveryNTicks(String roomCode, int n) {
        roomEveryNTicks.put(roomCode, Math.max(1, n));
    }

    public int getEveryNTicks(String roomCode) {
        return roomEveryNTicks.getOrDefault(roomCode, 1);
    }

    public void setClientEveryNTicks(String roomCode, String playerId, int n) {
        clientEveryNTicks.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>())
                .put(playerId, Math.max(1, n));
    }

    public int getClientEveryNTicks(String roomCode, String playerId) {
        Map<String, Integer> clientMap = clientEveryNTicks.get(roomCode);
        if (clientMap != null && clientMap.containsKey(playerId)) {
            return clientMap.get(playerId);
        }
        return getEveryNTicks(roomCode);
    }

    public boolean hasClientThrottles(String roomCode) {
        Map<String, Integer> clientMap = clientEveryNTicks.get(roomCode);
        return clientMap != null && !clientMap.isEmpty();
    }

    public void removePlayer(String roomCode, String playerId) {
        Map<String, Integer> clientMap = clientEveryNTicks.get(roomCode);
        if (clientMap != null) {
            clientMap.remove(playerId);
        }
        Map<String, java.util.concurrent.atomic.AtomicLong> counters = clientBroadcastCounters.get(roomCode);
        if (counters != null) {
            counters.remove(playerId);
        }
    }

    public void clearClient(String roomCode, String playerId) {
        removePlayer(roomCode, playerId);
    }

    public long incrementClientBroadcast(String roomCode, String playerId) {
        return clientBroadcastCounters.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(playerId, k -> new java.util.concurrent.atomic.AtomicLong(0))
                .incrementAndGet();
    }

    public long getClientBroadcastCount(String roomCode, String playerId) {
        Map<String, java.util.concurrent.atomic.AtomicLong> counters = clientBroadcastCounters.get(roomCode);
        if (counters != null && counters.containsKey(playerId)) {
            return counters.get(playerId).get();
        }
        return 0;
    }

    public Map<String, Integer> getAllClientThrottles(String roomCode) {
        Map<String, Integer> clientMap = clientEveryNTicks.get(roomCode);
        return clientMap != null ? Map.copyOf(clientMap) : Map.of();
    }
}
