package com.nexora.multiplayer.room;

import jakarta.persistence.*;
import java.time.Instant;

/** One user's (or bot's) membership in one GameRoom. Maps to the game_player table. */
@Entity
@Table(name = "game_player")
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private GameRoom room;

    /** Null for bot players — bots have no real user account. */
    @Column(name = "user_id", nullable = true)
    private Long userId;

    /** Display name snapshot, so the client never has to resolve userId -> name for gameplay. */
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "is_ready", nullable = false)
    private boolean ready = false;

    /** True for server-side AI bots added to fill empty slots. Bots do not contribute
     *  to telemetry RTT/jitter measurements — only real player network data is used. */
    @Column(name = "is_bot", nullable = false)
    private boolean bot = false;

    /** True for live observers/spectators who receive the state feed without submitting inputs. */
    @Column(name = "is_spectator", nullable = false)
    private boolean spectator = false;

    @Column(name = "ping_ms")
    private Integer pingMs;

    @Column(name = "reconnect_token", length = 64)
    private String reconnectToken;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "left_at")
    private Instant leftAt;

    protected GamePlayer() { }

    /** Constructor for real players. */
    public GamePlayer(GameRoom room, Long userId, String displayName) {
        this(room, userId, displayName, false);
    }

    /** Constructor for real players or spectators. */
    public GamePlayer(GameRoom room, Long userId, String displayName, boolean isSpectator) {
        this.room = room;
        this.userId = userId;
        this.displayName = displayName;
        this.bot = false;
        this.spectator = isSpectator;
        this.ready = isSpectator; // spectators are automatically ready
        this.reconnectToken = java.util.UUID.randomUUID().toString();
    }

    /** Constructor for bot players — userId is null; engine uses displayName as identity. */
    public GamePlayer(GameRoom room, String botDisplayName, boolean isBot) {
        this.room = room;
        this.userId = null;
        this.displayName = botDisplayName;
        this.bot = isBot;
        this.spectator = false;
        this.ready = true; // bots are always ready
    }

    public Long getId() { return id; }
    public GameRoom getRoom() { return room; }
    public Long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }
    public boolean isBot() { return bot; }
    public boolean isSpectator() { return spectator; }
    public void setSpectator(boolean spectator) { this.spectator = spectator; }
    public Integer getPingMs() { return pingMs; }
    public void setPingMs(Integer pingMs) { this.pingMs = pingMs; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLeftAt() { return leftAt; }
    public void markLeft() { this.leftAt = Instant.now(); }

    public String getReconnectToken() { return reconnectToken; }
    public void setReconnectToken(String reconnectToken) { this.reconnectToken = reconnectToken; }

    public Instant getDisconnectedAt() { return disconnectedAt; }
    public void setDisconnectedAt(Instant disconnectedAt) { this.disconnectedAt = disconnectedAt; }
    public boolean isTemporarilyDisconnected() { return disconnectedAt != null && leftAt == null; }
    public void markDisconnected() { this.disconnectedAt = Instant.now(); }
    public void markReconnected() { this.disconnectedAt = null; }
}
