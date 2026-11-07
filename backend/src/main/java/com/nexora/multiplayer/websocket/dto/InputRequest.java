package com.nexora.multiplayer.websocket.dto;

/** Wire shape for the PLAYER_INPUT message (docs/WEBSOCKET_PROTOCOL.md), mapped to
 *  com.nexora.multiplayer.engine.PlayerInput before reaching a game engine. No playerId
 *  field: identity comes from the authenticated STOMP session, never the message body —
 *  a client could otherwise move or fire as any other player just by changing this field. */
public class InputRequest {
    private long sequence;
    private long clientTimestamp;
    private double moveX;
    private double moveY;
    private double aimAngleRad;
    private boolean firing;
    private boolean actionPressed;

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
