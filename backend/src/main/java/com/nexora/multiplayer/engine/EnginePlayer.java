package com.nexora.multiplayer.engine;

/**
 * A player as seen by a game engine instance — deliberately decoupled from the
 * JPA GamePlayer entity so games never need a database dependency to run a tick.
 */
public class EnginePlayer {
    private final String playerId;   // stable id used in WebSocket messages (String form of userId)
    private final Long userId;
    private final String displayName;

    public EnginePlayer(String playerId, Long userId, String displayName) {
        this.playerId = playerId;
        this.userId = userId;
        this.displayName = displayName;
    }

    public String getPlayerId() { return playerId; }
    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
}
