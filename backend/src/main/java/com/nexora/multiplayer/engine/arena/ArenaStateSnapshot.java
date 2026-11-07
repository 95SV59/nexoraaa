package com.nexora.multiplayer.engine.arena;

import java.util.List;

/** Broadcast as the payload of a GAME_STATE WebSocket message (docs/WEBSOCKET_PROTOCOL.md). */
public class ArenaStateSnapshot {
    public final String roomId;
    public final long serverTimestamp;
    public final List<PlayerSnapshot> players;
    public final List<ProjectileSnapshot> projectiles;

    public ArenaStateSnapshot(String roomId, long serverTimestamp,
                               List<PlayerSnapshot> players, List<ProjectileSnapshot> projectiles) {
        this.roomId = roomId;
        this.serverTimestamp = serverTimestamp;
        this.players = players;
        this.projectiles = projectiles;
    }

    public static class PlayerSnapshot {
        public final String playerId;
        public final String displayName;
        public final double x, y;
        public final double aimAngleRad;
        public final int health;
        public final boolean alive;
        public final int score;

        public PlayerSnapshot(String playerId, String displayName, double x, double y,
                               double aimAngleRad, int health, boolean alive, int score) {
            this.playerId = playerId;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.aimAngleRad = aimAngleRad;
            this.health = health;
            this.alive = alive;
            this.score = score;
        }
    }

    public static class ProjectileSnapshot {
        public final long id;
        public final double x, y;

        public ProjectileSnapshot(long id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }
}
