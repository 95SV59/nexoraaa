package com.nexora.multiplayer.engine.arena;

import com.nexora.multiplayer.engine.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * GAME 1 — NEXORA ARENA. 2D multiplayer shooter, 2-8 players.
 * State: position, aim, projectiles, health, player state, high-frequency movement, events.
 * Patent-relevant surface: high-frequency sync, state prioritization, position consistency,
 * adaptive update frequency, packet aggregation, prediction-error feedback (docs/
 * PATENT_TECHNICAL_DOCUMENT.md) — this class is the game under test, not the controller itself.
 *
 * Thread-safety: processInput() is called from WebSocket-handling threads while updateState()
 * runs on GameLoopManager's per-room scheduled thread; both synchronize on `lock`.
 */
public class NexoraArenaGame implements MultiplayerGame {

    private final Object lock = new Object();
    private final Map<String, ArenaPlayerState> players = new ConcurrentHashMap<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final AtomicLong projectileIds = new AtomicLong(1);

    private String roomId;
    private long matchStartMillis;
    private boolean matchOver = false;
    private String winnerPlayerId;

    @Override
    public void initialize(String roomId, List<EnginePlayer> roster) {
        synchronized (lock) {
            this.roomId = roomId;
            this.matchStartMillis = System.currentTimeMillis();
            this.matchOver = false;
            this.winnerPlayerId = null;
            players.clear();
            projectiles.clear();
            int i = 0;
            for (EnginePlayer p : roster) {
                spawnPlayer(p, i++, roster.size());
            }
        }
    }

    @Override
    public void addPlayer(EnginePlayer player) {
        synchronized (lock) {
            if (players.size() >= ArenaConstants.MAX_PLAYERS || matchOver) return;
            spawnPlayer(player, players.size(), Math.max(players.size() + 1, ArenaConstants.MIN_PLAYERS));
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
        ArenaPlayerState state = players.get(playerId);
        if (state == null) return;
        synchronized (lock) {
            state.pendingMoveX = clamp(input.getMoveX(), -1, 1);
            state.pendingMoveY = clamp(input.getMoveY(), -1, 1);
            state.aimAngleRad = input.getAimAngleRad();
            state.pendingFire = input.isFiring();
        }
    }

    @Override
    public void updateState() {
        synchronized (lock) {
            if (matchOver) return;
            double dt = 1.0 / ArenaConstants.TICK_RATE_HZ;
            long now = System.currentTimeMillis();

            for (ArenaPlayerState p : players.values()) {
                if (!p.alive) {
                    if (now >= p.respawnAtMillis) {
                        int idx = new ArrayList<>(players.keySet()).indexOf(p.player.getPlayerId());
                        double[] spawn = spawnPoint(idx, players.size());
                        p.respawn(spawn[0], spawn[1]);
                    }
                    continue;
                }
                applyMovement(p, dt);
                tryFire(p, now);
            }

            advanceProjectiles(dt);
            resolveCollisions(now);
            checkWinCondition();
        }
    }

    @Override
    public boolean validateState() {
        synchronized (lock) {
            for (ArenaPlayerState p : players.values()) {
                if (p.x < 0 || p.x > ArenaConstants.ARENA_WIDTH || p.y < 0 || p.y > ArenaConstants.ARENA_HEIGHT) {
                    return false;
                }
                if (p.health < 0 || p.health > ArenaConstants.PLAYER_MAX_HEALTH) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public Object getStateSnapshot() {
        synchronized (lock) {
            List<ArenaStateSnapshot.PlayerSnapshot> playerSnaps = players.values().stream()
                    .map(p -> new ArenaStateSnapshot.PlayerSnapshot(
                            p.player.getPlayerId(), p.player.getDisplayName(),
                            p.x, p.y, p.aimAngleRad, p.health, p.alive, p.score))
                    .collect(Collectors.toList());
            List<ArenaStateSnapshot.ProjectileSnapshot> projSnaps = projectiles.stream()
                    .map(pr -> new ArenaStateSnapshot.ProjectileSnapshot(pr.id, pr.x, pr.y))
                    .collect(Collectors.toList());
            return new ArenaStateSnapshot(roomId, System.currentTimeMillis(), playerSnaps, projSnaps);
        }
    }

    @Override
    public List<StateElementMetadata> getStateMetadata() {
        // Tolerances are design values reasoned from the game's own physics, not fitted to a
        // network profile: projectiles move at 620 px/s, so 80 ms of staleness is ~50 px of
        // visible error (about two player radii) and 12 px of divergence is about half the hit
        // radius. The original 50 ms / 5 px left only ~17 ms of network slack at a 33 ms tick,
        // which no real connection could meet. Aim's rate (4 rad/s) is a declared assumption
        // for a fast mouse flick, in radians because that is what its tolerance is in.
        return List.of(
                new StateElementMetadata("player_position", StatePriority.HIGH,
                        ArenaConstants.TICK_RATE_HZ, 100, ArenaConstants.PLAYER_RADIUS,
                        ArenaConstants.PLAYER_SPEED_PER_SEC, true, false, false),
                new StateElementMetadata("player_aim", StatePriority.MODERATE,
                        20, 150, 0.3, 4.0, true, false, true),
                new StateElementMetadata("projectile_position", StatePriority.CRITICAL,
                        ArenaConstants.TICK_RATE_HZ, 80, 12,
                        ArenaConstants.PROJECTILE_SPEED_PER_SEC, true, false, false),
                new StateElementMetadata("player_health", StatePriority.CRITICAL,
                        0, 0, 0, 0, false, false, false),
                new StateElementMetadata("player_alive_state", StatePriority.HIGH,
                        0, 0, 0, 0, false, false, false),
                new StateElementMetadata("score", StatePriority.CRITICAL,
                        0, 0, 0, 0, false, false, false)
        );
    }

    @Override
    public boolean isMatchOver() {
        synchronized (lock) {
            return matchOver;
        }
    }

    @Override
    public MatchResultData endMatch() {
        synchronized (lock) {
            int durationSec = (int) ((System.currentTimeMillis() - matchStartMillis) / 1000);
            Map<String, Object> summary = new LinkedHashMap<>();
            for (ArenaPlayerState p : players.values()) {
                summary.put(p.player.getPlayerId(), Map.of(
                        "displayName", p.player.getDisplayName(),
                        "score", p.score
                ));
            }
            return new MatchResultData(winnerPlayerId, durationSec, summary);
        }
    }

    @Override
    public int getTickRateHz() {
        return ArenaConstants.TICK_RATE_HZ;
    }

    // ---- internal mechanics ----

    private void spawnPlayer(EnginePlayer player, int index, int totalForSpread) {
        double[] spawn = spawnPoint(index, Math.max(totalForSpread, ArenaConstants.MIN_PLAYERS));
        players.put(player.getPlayerId(), new ArenaPlayerState(player, spawn[0], spawn[1]));
    }

    /** Evenly spread spawn points around a circle centered in the arena. */
    private double[] spawnPoint(int index, int total) {
        double cx = ArenaConstants.ARENA_WIDTH / 2.0;
        double cy = ArenaConstants.ARENA_HEIGHT / 2.0;
        double radius = Math.min(cx, cy) * 0.7;
        double angle = (2 * Math.PI * Math.max(index, 0)) / Math.max(total, 1);
        return new double[]{cx + radius * Math.cos(angle), cy + radius * Math.sin(angle)};
    }

    private void applyMovement(ArenaPlayerState p, double dt) {
        double mx = p.pendingMoveX, my = p.pendingMoveY;
        double mag = Math.sqrt(mx * mx + my * my);
        if (mag > 1e-6) {
            mx /= mag;
            my /= mag;
            p.x = clamp(p.x + mx * ArenaConstants.PLAYER_SPEED_PER_SEC * dt,
                    ArenaConstants.PLAYER_RADIUS, ArenaConstants.ARENA_WIDTH - ArenaConstants.PLAYER_RADIUS);
            p.y = clamp(p.y + my * ArenaConstants.PLAYER_SPEED_PER_SEC * dt,
                    ArenaConstants.PLAYER_RADIUS, ArenaConstants.ARENA_HEIGHT - ArenaConstants.PLAYER_RADIUS);
        }
    }

    private void tryFire(ArenaPlayerState p, long now) {
        if (!p.pendingFire) return;
        if (now - p.lastFiredAtMillis < ArenaConstants.FIRE_COOLDOWN_MS) return;
        p.lastFiredAtMillis = now;
        double vx = Math.cos(p.aimAngleRad) * ArenaConstants.PROJECTILE_SPEED_PER_SEC;
        double vy = Math.sin(p.aimAngleRad) * ArenaConstants.PROJECTILE_SPEED_PER_SEC;
        double spawnX = p.x + Math.cos(p.aimAngleRad) * (ArenaConstants.PLAYER_RADIUS + 2);
        double spawnY = p.y + Math.sin(p.aimAngleRad) * (ArenaConstants.PLAYER_RADIUS + 2);
        projectiles.add(new Projectile(projectileIds.getAndIncrement(), p.player.getPlayerId(),
                spawnX, spawnY, vx, vy));
    }

    private void advanceProjectiles(double dt) {
        Iterator<Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            Projectile pr = it.next();
            pr.x += pr.vx * dt;
            pr.y += pr.vy * dt;
            pr.ageSec += dt;
            boolean outOfBounds = pr.x < 0 || pr.x > ArenaConstants.ARENA_WIDTH
                    || pr.y < 0 || pr.y > ArenaConstants.ARENA_HEIGHT;
            if (outOfBounds || pr.ageSec > ArenaConstants.PROJECTILE_MAX_LIFETIME_SEC) {
                it.remove();
            }
        }
    }

    private void resolveCollisions(long now) {
        Iterator<Projectile> it = projectiles.iterator();
        double hitDistSq = Math.pow(ArenaConstants.PLAYER_RADIUS + ArenaConstants.PROJECTILE_RADIUS, 2);
        while (it.hasNext()) {
            Projectile pr = it.next();
            for (ArenaPlayerState target : players.values()) {
                if (!target.alive) continue;
                if (target.player.getPlayerId().equals(pr.ownerPlayerId)) continue; // no self-hit
                double dx = target.x - pr.x, dy = target.y - pr.y;
                if (dx * dx + dy * dy <= hitDistSq) {
                    target.takeDamage(ArenaConstants.PROJECTILE_DAMAGE);
                    it.remove();
                    if (!target.alive) {
                        target.respawnAtMillis = now + ArenaConstants.RESPAWN_DELAY_MS;
                        ArenaPlayerState shooter = players.get(pr.ownerPlayerId);
                        if (shooter != null) {
                            shooter.score += 1;
                        }
                    }
                    break; // this projectile is consumed
                }
            }
        }
    }

    private void checkWinCondition() {
        for (ArenaPlayerState p : players.values()) {
            if (p.score >= ArenaConstants.SCORE_TO_WIN) {
                matchOver = true;
                winnerPlayerId = p.player.getPlayerId();
                return;
            }
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
