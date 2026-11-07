package com.nexora.multiplayer.engine.arena;

import com.nexora.multiplayer.engine.EnginePlayer;

/** Live per-player state for one Arena match. */
public class ArenaPlayerState {
    public final EnginePlayer player;
    public double x, y;
    public double aimAngleRad;
    public int health = ArenaConstants.PLAYER_MAX_HEALTH;
    public int score = 0;
    public boolean alive = true;
    public long respawnAtMillis = 0;
    public long lastFiredAtMillis = 0;

    // buffered input, applied once per tick
    public double pendingMoveX = 0;
    public double pendingMoveY = 0;
    public boolean pendingFire = false;

    public ArenaPlayerState(EnginePlayer player, double x, double y) {
        this.player = player;
        this.x = x;
        this.y = y;
    }

    public void takeDamage(int amount) {
        if (!alive) return;
        health -= amount;
        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    public void respawn(double spawnX, double spawnY) {
        this.x = spawnX;
        this.y = spawnY;
        this.health = ArenaConstants.PLAYER_MAX_HEALTH;
        this.alive = true;
    }
}
