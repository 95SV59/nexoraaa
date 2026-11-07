package com.nexora.multiplayer.transport;

/**
 * Patent Stage 207 & Research Requirement §7.5 / Claim 8:
 * Decouples game state broadcasting from underlying transport protocols (STOMP WebSocket vs WebRTC DataChannel).
 * Allows comparative benchmarking of TCP head-of-line blocking vs. unreliable datagram packet loss.
 */
public interface MultiplayerTransportGateway {

    /**
     * Broadcasts continuous game simulation snapshots to room participants.
     */
    void broadcastState(String roomCode, Object stateSnapshot, TransportChannel channel);

    /**
     * Broadcasts critical discrete match lifecycle events (START, END, RECONNECT).
     * Always delivered reliably.
     */
    void broadcastLifecycle(String roomCode, Object lifecycleEvent);

    /**
     * Broadcasts lobby readiness and player roster changes reliably.
     */
    void broadcastLobby(String roomCode, Object lobbyUpdate);

    /**
     * Broadcasts server-initiated latency probe pings reliably.
     */
    void broadcastPing(String roomCode, Object pingBroadcast);

    /**
     * Broadcasts PWNC decision and what-if telemetry snapshots.
     */
    void broadcastNetworkSnapshot(String roomCode, Object snapshot);

    /**
     * Sends message to an individual player.
     */
    void sendToPlayer(String roomCode, String playerId, String destination, Object payload, TransportChannel channel);

    /**
     * Returns the active channel for the given room.
     */
    TransportChannel getActiveChannel(String roomCode);

    /**
     * Sets the active transport channel for the given room.
     */
    void setActiveChannel(String roomCode, TransportChannel channel);
}
