package com.nexora.multiplayer.engine.tag;

public final class TagConstants {
    private TagConstants() { }

    public static final int TICK_RATE_HZ = 30;
    public static final int MIN_PLAYERS = 1;
    public static final int MAX_PLAYERS = 10;
    public static final int MATCH_DURATION_SEC = 60;

    public static final double ARENA_WIDTH = 800.0;
    public static final double ARENA_HEIGHT = 600.0;

    public static final double RUNNER_SPEED = 200.0;
    public static final double IT_SPEED = 225.0; // slight boost for IT
    public static final double PLAYER_RADIUS = 16.0;
    public static final double TAG_DISTANCE = 32.0;
    public static final long TAG_IMMUNITY_MS = 1500;
}
