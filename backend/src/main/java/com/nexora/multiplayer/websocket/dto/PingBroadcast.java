package com.nexora.multiplayer.websocket.dto;

/** Broadcast to /topic/room/{roomCode}/ping. Every connected client echoes it straight
 *  back via /app/room/{roomCode}/pong so RTT is measured entirely in server time —
 *  no client clock to trust (docs/ARCHITECTURE.md §8 spirit applied to telemetry too). */
public class PingBroadcast {
    public long pingId;
    public long serverSendTime;

    public PingBroadcast(long pingId, long serverSendTime) {
        this.pingId = pingId;
        this.serverSendTime = serverSendTime;
    }
}
