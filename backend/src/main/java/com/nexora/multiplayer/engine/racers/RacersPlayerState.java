package com.nexora.multiplayer.engine.racers;

import com.nexora.multiplayer.engine.EnginePlayer;

public class RacersPlayerState {
    public final EnginePlayer player;
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double headingAngleRad;
    public double currentSpeed;

    // Controls
    public double pendingThrottle; // -1 (reverse/brake) to +1 (forward)
    public double pendingSteer;    // -1 (left) to +1 (right)

    // Race Progress
    public int currentLap = 1;
    public int nextCheckpoint = 1; // 0 is start/finish
    public boolean finished = false;
    public long finishTimeMs = 0;
    public int place = 0;

    public RacersPlayerState(EnginePlayer player, double startX, double startY, double heading) {
        this.player = player;
        this.x = startX;
        this.y = startY;
        this.headingAngleRad = heading;
        this.currentSpeed = 0;
        this.vx = 0;
        this.vy = 0;
    }
}
