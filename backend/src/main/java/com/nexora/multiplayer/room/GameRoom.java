package com.nexora.multiplayer.room;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A lobby/match container for one game instance. Maps to the game_room table
 * (see docs/DATABASE_DESIGN.md). Room membership lives in GamePlayer.
 */
@Entity
@Table(name = "game_room")
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, unique = true, length = 10)
    private String roomCode;

    /** Matches Game.slug (e.g. "arena") — kept as a plain string here so the
     *  multiplayer package does not need a hard dependency on the catalogue package. */
    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "host_user_id", nullable = false)
    private Long hostUserId;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "current_players", nullable = false)
    private int currentPlayers = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status = RoomStatus.LOBBY;

    private String region;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected GameRoom() { }

    public GameRoom(String roomCode, String gameId, Long hostUserId, int maxPlayers, String region) {
        this.roomCode = roomCode;
        this.gameId = gameId;
        this.hostUserId = hostUserId;
        this.maxPlayers = maxPlayers;
        this.region = region;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomCode() { return roomCode; }
    public String getGameId() { return gameId; }
    public Long getHostUserId() { return hostUserId; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
    public String getRegion() { return region; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isFull() { return currentPlayers >= maxPlayers; }
}
