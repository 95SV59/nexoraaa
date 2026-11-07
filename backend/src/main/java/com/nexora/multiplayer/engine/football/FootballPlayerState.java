package com.nexora.multiplayer.engine.football;

import com.nexora.multiplayer.engine.EnginePlayer;

public class FootballPlayerState {
    public final EnginePlayer player;
    public double x;
    public double y;
    public double vx;
    public double vy;
    public double aimAngleRad;
    public String team; // "RED" or "BLUE"
    public boolean hasBall = false;

    // Inputs
    public double pendingMoveX;
    public double pendingMoveY;
    public boolean pendingKick;

    public long tackleCooldownUntil = 0;

    public FootballPlayerState(EnginePlayer player, double x, double y, String team) {
        this.player = player;
        this.x = x;
        this.y = y;
        this.team = team;
    }
}
