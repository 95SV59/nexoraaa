package com.nexora.multiplayer.engine.football;

public final class FootballConstants {
    private FootballConstants() { }

    public static final int TICK_RATE_HZ = 30;
    public static final int MIN_PLAYERS = 1;
    public static final int MAX_PLAYERS = 6;
    public static final int MATCH_DURATION_SEC = 90;
    public static final int SCORE_LIMIT = 5;

    public static final double PITCH_WIDTH = 900.0;
    public static final double PITCH_HEIGHT = 550.0;

    public static final double GOAL_Y_MIN = 190.0;
    public static final double GOAL_Y_MAX = 360.0;
    public static final double GOAL_LINE_LEFT = 60.0;
    public static final double GOAL_LINE_RIGHT = 840.0;

    public static final double PLAYER_RADIUS = 18.0;
    public static final double BALL_RADIUS = 10.0;
    public static final double PLAYER_SPEED = 180.0;
    public static final double BALL_FRICTION = 140.0; // px/s^2 deceleration
    public static final double KICK_SPEED = 380.0;
}
