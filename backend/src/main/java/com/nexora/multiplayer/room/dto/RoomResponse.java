package com.nexora.multiplayer.room.dto;

import com.nexora.multiplayer.room.GameRoom;
import com.nexora.multiplayer.room.GamePlayer;
import java.util.List;
import java.util.stream.Collectors;

public class RoomResponse {
    public Long id;
    public String roomCode;
    public String gameId;
    public Long hostUserId;
    public int maxPlayers;
    public String status;
    public String reconnectToken;
    public List<PlayerSummary> players;

    public static RoomResponse from(GameRoom room, List<GamePlayer> players) {
        return from(room, players, null);
    }

    public static RoomResponse from(GameRoom room, List<GamePlayer> players, Long currentUserId) {
        RoomResponse r = new RoomResponse();
        r.id = room.getId();
        r.roomCode = room.getRoomCode();
        r.gameId = room.getGameId();
        r.hostUserId = room.getHostUserId();
        r.maxPlayers = room.getMaxPlayers();
        r.status = room.getStatus().name();
        r.players = players.stream()
                .map(p -> new PlayerSummary(p.getUserId(), p.getDisplayName(), p.isReady(), p.isBot(), p.getPingMs()))
                .collect(Collectors.toList());

        if (currentUserId != null) {
            players.stream()
                    .filter(p -> currentUserId.equals(p.getUserId()))
                    .findFirst()
                    .ifPresent(p -> r.reconnectToken = p.getReconnectToken());
        }
        return r;
    }
}
