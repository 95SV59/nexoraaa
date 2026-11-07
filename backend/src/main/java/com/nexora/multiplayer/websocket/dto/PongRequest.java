package com.nexora.multiplayer.websocket.dto;

/** No playerId field: identity comes from the authenticated STOMP session, never the
 *  message body — see GameWebSocketController. */
public class PongRequest {
    private long pingId;
    private long serverSendTime; // echoed back unchanged from the PingBroadcast

    public long getPingId() { return pingId; }
    public void setPingId(long pingId) { this.pingId = pingId; }
    public long getServerSendTime() { return serverSendTime; }
    public void setServerSendTime(long serverSendTime) { this.serverSendTime = serverSendTime; }
}
