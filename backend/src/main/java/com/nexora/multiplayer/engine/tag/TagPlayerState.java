package com.nexora.multiplayer.engine.tag;

import com.nexora.multiplayer.engine.EnginePlayer;

public class TagPlayerState {
    public final EnginePlayer player;
    public double x;
    public double y;
    public double vx;
    public double vy;

    public boolean isIt = false;
    public double timeAsItSec = 0.0;
    public long immuneUntilMs = 0;
    public int tagsMade = 0;

    // Inputs
    public double pendingMoveX;
    public double pendingMoveY;

    public TagPlayerState(EnginePlayer player, double x, double y) {
        this.player = player;
        this.x = x;
        this.y = y;
    }
}
