package com.nexora.multiplayer.engine.racers;

import com.nexora.multiplayer.engine.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * GAME 2 — NEXORA RACERS. Multiplayer kart racing for 2-8 players.
 * State: vehicle position, velocity, acceleration, steering, checkpoints, laps, race position.
 * Patent-relevant surface: dead reckoning, movement prediction, jitter and update-frequency experiments.
 */
public class NexoraRacersGame implements MultiplayerGame {

    private final Object lock = new Object();
    private final Map<String, RacersPlayerState> players = new ConcurrentHashMap<>();

    private String roomId;
    private long matchStartMillis;
    private boolean matchOver = false;
    private String winnerPlayerId;
    private int finishCount = 0;

    @Override
    public void initialize(String roomId, List<EnginePlayer> roster) {
        synchronized (lock) {
            this.roomId = roomId;
            this.matchStartMillis = System.currentTimeMillis();
            this.matchOver = false;
            this.winnerPlayerId = null;
            this.finishCount = 0;
            players.clear();

            int i = 0;
            for (EnginePlayer p : roster) {
                spawnPlayer(p, i++, roster.size());
            }
        }
    }

    @Override
    public void addPlayer(EnginePlayer player) {
        synchronized (lock) {
            if (players.size() >= RacersConstants.MAX_PLAYERS || matchOver) return;
            spawnPlayer(player, players.size(), Math.max(players.size() + 1, RacersConstants.MIN_PLAYERS));
        }
    }

    @Override
    public void removePlayer(String playerId) {
        synchronized (lock) {
            players.remove(playerId);
        }
    }

    @Override
    public void processInput(String playerId, PlayerInput input) {
        RacersPlayerState p = players.get(playerId);
        if (p == null) return;
        synchronized (lock) {
            // moveY > 0 -> accelerate forward; moveY < 0 -> reverse / brake
            p.pendingThrottle = clamp(-input.getMoveY(), -1.0, 1.0);
            p.pendingSteer = clamp(input.getMoveX(), -1.0, 1.0);
        }
    }

    @Override
    public void updateState() {
        synchronized (lock) {
            if (matchOver) return;
            double dt = 1.0 / RacersConstants.TICK_RATE_HZ;
            long now = System.currentTimeMillis();

            for (RacersPlayerState p : players.values()) {
                if (p.finished) continue;

                // Update heading based on steer and speed
                double speedRatio = Math.abs(p.currentSpeed) / RacersConstants.MAX_SPEED;
                p.headingAngleRad += p.pendingSteer * RacersConstants.TURN_SPEED * dt * Math.max(speedRatio, 0.25);

                // Update speed based on throttle and friction
                if (p.pendingThrottle > 0.05) {
                    p.currentSpeed = Math.min(RacersConstants.MAX_SPEED, p.currentSpeed + RacersConstants.ACCELERATION * p.pendingThrottle * dt);
                } else if (p.pendingThrottle < -0.05) {
                    p.currentSpeed = Math.max(-RacersConstants.MAX_SPEED * 0.4, p.currentSpeed - RacersConstants.BRAKE_DECEL * dt);
                } else {
                    // Friction
                    if (p.currentSpeed > 0) {
                        p.currentSpeed = Math.max(0, p.currentSpeed - RacersConstants.FRICTION * dt);
                    } else if (p.currentSpeed < 0) {
                        p.currentSpeed = Math.min(0, p.currentSpeed + RacersConstants.FRICTION * dt);
                    }
                }

                // Velocities
                p.vx = p.currentSpeed * Math.cos(p.headingAngleRad);
                p.vy = p.currentSpeed * Math.sin(p.headingAngleRad);

                p.x = clamp(p.x + p.vx * dt, 40.0, RacersConstants.TRACK_WIDTH - 40.0);
                p.y = clamp(p.y + p.vy * dt, 40.0, RacersConstants.TRACK_HEIGHT - 40.0);

                // Checkpoint check
                double[] targetCp = RacersConstants.CHECKPOINTS[p.nextCheckpoint];
                double distSq = (p.x - targetCp[0]) * (p.x - targetCp[0]) + (p.y - targetCp[1]) * (p.y - targetCp[1]);
                double radiusSq = targetCp[2] * targetCp[2];

                if (distSq <= radiusSq) {
                    p.nextCheckpoint = (p.nextCheckpoint + 1) % RacersConstants.CHECKPOINTS.length;
                    if (p.nextCheckpoint == 1) { // Completed a lap (passed CP 0)
                        p.currentLap++;
                        if (p.currentLap > RacersConstants.TOTAL_LAPS) {
                            p.finished = true;
                            p.finishTimeMs = now - matchStartMillis;
                            p.place = ++finishCount;
                            if (winnerPlayerId == null) {
                                winnerPlayerId = p.player.getPlayerId();
                            }
                        }
                    }
                }
            }

            // Check if all players finished or match timeout (2 minutes)
            boolean allFinished = !players.isEmpty() && players.values().stream().allMatch(p -> p.finished);
            boolean timedOut = (now - matchStartMillis) > 120_000;
            if (allFinished || timedOut) {
                matchOver = true;
                if (winnerPlayerId == null && !players.isEmpty()) {
                    winnerPlayerId = players.values().stream()
                            .max(Comparator.comparingInt((RacersPlayerState p) -> p.currentLap)
                                    .thenComparingInt(p -> p.nextCheckpoint))
                            .map(p -> p.player.getPlayerId())
                            .orElse(null);
                }
            }
        }
    }

    @Override
    public boolean validateState() {
        for (RacersPlayerState p : players.values()) {
            if (Double.isNaN(p.x) || Double.isNaN(p.y) || Math.abs(p.currentSpeed) > RacersConstants.MAX_SPEED * 1.5) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object getStateSnapshot() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            List<RacersStateSnapshot.VehicleSnapshot> list = players.values().stream()
                    .map(p -> new RacersStateSnapshot.VehicleSnapshot(
                            p.player.getPlayerId(),
                            p.player.getDisplayName(),
                            round(p.x),
                            round(p.y),
                            round(p.vx),
                            round(p.vy),
                            round(p.headingAngleRad),
                            round(p.currentSpeed),
                            p.currentLap,
                            p.nextCheckpoint,
                            p.finished,
                            p.place
                    ))
                    .collect(Collectors.toList());

            return new RacersStateSnapshot(
                    roomId,
                    now,
                    list,
                    matchOver,
                    winnerPlayerId,
                    now - matchStartMillis
            );
        }
    }

    @Override
    public List<StateElementMetadata> getStateMetadata() {
        return List.of(
                new StateElementMetadata("vehicle_position", StatePriority.HIGH, 30.0, 80, 6.0, 280.0, true, true, true),
                new StateElementMetadata("vehicle_velocity", StatePriority.HIGH, 20.0, 120, 2.0, 50.0, true, true, true),
                new StateElementMetadata("lap_checkpoint", StatePriority.CRITICAL, 10.0, 200, 0.0, 1.0, false, false, false),
                new StateElementMetadata("race_position", StatePriority.MODERATE, 5.0, 300, 0.0, 1.0, false, false, false)
        );
    }

    @Override
    public boolean isMatchOver() {
        return matchOver;
    }

    @Override
    public MatchResultData endMatch() {
        synchronized (lock) {
            this.matchOver = true;
            int durationSec = (int) Math.max(1, (System.currentTimeMillis() - matchStartMillis) / 1000);
            Map<String, Object> summary = new LinkedHashMap<>();
            List<Map<String, Object>> standings = players.values().stream()
                    .sorted(Comparator.comparingInt((RacersPlayerState p) -> p.place == 0 ? 999 : p.place)
                            .thenComparingInt(p -> -p.currentLap))
                    .map(p -> Map.<String, Object>of(
                            "playerId", p.player.getPlayerId(),
                            "displayName", p.player.getDisplayName(),
                            "place", p.place,
                            "lap", p.currentLap,
                            "finishTimeMs", p.finishTimeMs
                    ))
                    .collect(Collectors.toList());

            summary.put("game", "racers");
            summary.put("standings", standings);
            return new MatchResultData(winnerPlayerId, durationSec, summary);
        }
    }

    @Override
    public int getTickRateHz() {
        return RacersConstants.TICK_RATE_HZ;
    }

    private void spawnPlayer(EnginePlayer p, int index, int total) {
        // Line vehicles up at starting grid near CP 0
        double startX = 400.0 + (index % 2 == 0 ? -30.0 : 30.0);
        double startY = 100.0 + (index / 2) * 40.0;
        double heading = 0.0; // facing right towards CP 1
        players.put(p.getPlayerId(), new RacersPlayerState(p, startX, startY, heading));
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
