package com.nexora.multiplayer.websocket.dto;

/** Broadcast to /topic/room/{roomCode}/lifecycle. event is "GAME_START" or "GAME_END".
 *  sessionId is what a Phase 20 comparison (GET /api/analysis/compare) is keyed on, so
 *  clients can show it to whoever is running the experiment. */
public class MatchLifecycleMessage {
    public String roomCode;
    public String event;
    public Object matchResult; // populated only for GAME_END
    public Long sessionId;

    public MatchLifecycleMessage(String roomCode, String event, Object matchResult, Long sessionId) {
        this.roomCode = roomCode;
        this.event = event;
        this.matchResult = matchResult;
        this.sessionId = sessionId;
    }
}
