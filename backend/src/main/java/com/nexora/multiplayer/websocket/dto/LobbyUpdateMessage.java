package com.nexora.multiplayer.websocket.dto;

import com.nexora.multiplayer.room.dto.PlayerSummary;
import java.util.List;

/** Broadcast to /topic/room/{roomCode}/lobby whenever membership or ready-state changes. */
public class LobbyUpdateMessage {
    public String roomCode;
    public String status;
    public List<PlayerSummary> players;

    public LobbyUpdateMessage(String roomCode, String status, List<PlayerSummary> players) {
        this.roomCode = roomCode;
        this.status = status;
        this.players = players;
    }
}
