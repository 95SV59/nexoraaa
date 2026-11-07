package com.nexora.multiplayer.websocket.dto;

/** No userId field: identity comes from the authenticated STOMP session (see
 *  GameWebSocketController), never from the message body. */
public class ReadyRequest {
    private boolean ready;

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
}
