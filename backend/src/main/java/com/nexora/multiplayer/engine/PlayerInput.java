package com.nexora.multiplayer.engine;

/**
 * Raw input from one client for one tick. Games interpret the fields relevant to
 * them; unused fields are simply ignored (e.g. Chess Arena ignores moveX/moveY).
 * Mirrors the PLAYER_INPUT message in docs/WEBSOCKET_PROTOCOL.md.
 */
public class PlayerInput {
    private String playerId;
    private long sequence;
    private long clientTimestamp;

    private double moveX;      // -1..1
    private double moveY;      // -1..1
    private double aimAngleRad;
    private boolean firing;
    private boolean actionPressed; // generic secondary action (reload, interact, etc.)

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
    public long getClientTimestamp() { return clientTimestamp; }
    public void setClientTimestamp(long clientTimestamp) { this.clientTimestamp = clientTimestamp; }
    public double getMoveX() { return moveX; }
    public void setMoveX(double moveX) { this.moveX = moveX; }
    public double getMoveY() { return moveY; }
    public void setMoveY(double moveY) { this.moveY = moveY; }
    public double getAimAngleRad() { return aimAngleRad; }
    public void setAimAngleRad(double aimAngleRad) { this.aimAngleRad = aimAngleRad; }
    public boolean isFiring() { return firing; }
    public void setFiring(boolean firing) { this.firing = firing; }
    public boolean isActionPressed() { return actionPressed; }
    public void setActionPressed(boolean actionPressed) { this.actionPressed = actionPressed; }
}
