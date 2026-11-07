package com.nexora.multiplayer.engine.racers;

public final class RacersConstants {
    private RacersConstants() { }

    public static final int TICK_RATE_HZ = 30;
    public static final int MIN_PLAYERS = 1; // allow single player testing & multi
    public static final int MAX_PLAYERS = 8;
    public static final int TOTAL_LAPS = 3;
    public static final double TRACK_WIDTH = 900.0;
    public static final double TRACK_HEIGHT = 600.0;

    public static final double MAX_SPEED = 280.0;
    public static final double ACCELERATION = 200.0;
    public static final double BRAKE_DECEL = 300.0;
    public static final double FRICTION = 60.0;
    public static final double TURN_SPEED = 3.2; // rad/s

    // Checkpoints on elliptical track: (x, y, radius)
    public static final double[][] CHECKPOINTS = {
            {450.0, 100.0, 80.0}, // CP 0 - Finish Line / Top
            {780.0, 300.0, 80.0}, // CP 1 - Right Turn
            {450.0, 500.0, 80.0}, // CP 2 - Bottom Straight
            {120.0, 300.0, 80.0}  // CP 3 - Left Turn
    };
}
