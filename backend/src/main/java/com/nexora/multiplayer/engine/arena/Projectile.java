package com.nexora.multiplayer.engine.arena;

public class Projectile {
    public final long id;
    public final String ownerPlayerId;
    public double x, y;
    public final double vx, vy;
    public double ageSec = 0;

    public Projectile(long id, String ownerPlayerId, double x, double y, double vx, double vy) {
        this.id = id;
        this.ownerPlayerId = ownerPlayerId;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }
}
