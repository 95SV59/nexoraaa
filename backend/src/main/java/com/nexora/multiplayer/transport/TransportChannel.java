package com.nexora.multiplayer.transport;

/**
 * Identifies the delivery semantics of a multiplayer network channel:
 * - RELIABLE_ORDERED: STOMP over WebSocket (TCP) with guaranteed delivery and sequence ordering.
 * - UNRELIABLE_UNORDERED: WebRTC DataChannel (UDP / SCTP) for latency-critical continuous game state snapshots.
 */
public enum TransportChannel {
    RELIABLE_ORDERED,
    UNRELIABLE_UNORDERED
}
