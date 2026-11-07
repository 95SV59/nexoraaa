package com.nexora.multiplayer.engine.arena;

/** Tunable constants for NEXORA Arena (Game 1 — 2D multiplayer shooter, 2-8 players). */
public final class ArenaConstants {
    private ArenaConstants() { }

    public static final int TICK_RATE_HZ = 30;
    public static final double ARENA_WIDTH = 1600;
    public static final double ARENA_HEIGHT = 900;

    public static final double PLAYER_RADIUS = 18;
    public static final double PLAYER_SPEED_PER_SEC = 260;
    public static final int PLAYER_MAX_HEALTH = 100;
    public static final long RESPAWN_DELAY_MS = 3000;

    public static final double PROJECTILE_RADIUS = 5;
    public static final double PROJECTILE_SPEED_PER_SEC = 620;
    public static final int PROJECTILE_DAMAGE = 20;
    public static final long FIRE_COOLDOWN_MS = 260;
    public static final double PROJECTILE_MAX_LIFETIME_SEC = 2.0;

    public static final int SCORE_TO_WIN = 10;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 8;
}
