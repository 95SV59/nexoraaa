package com.nexora.social.presence;

import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_presence")
public class UserPresence {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PresenceStatus status = PresenceStatus.OFFLINE;

    @Column(name = "current_game_slug", length = 60)
    private String currentGameSlug;

    @Column(name = "current_room_code", length = 10)
    private String currentRoomCode;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen = LocalDateTime.now();

    public UserPresence() {}

    public UserPresence(User user, PresenceStatus status) {
        this.user = user;
        this.userId = user.getId();
        this.status = status;
        this.lastSeen = LocalDateTime.now();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public PresenceStatus getStatus() { return status; }
    public void setStatus(PresenceStatus status) { this.status = status; }

    public String getCurrentGameSlug() { return currentGameSlug; }
    public void setCurrentGameSlug(String currentGameSlug) { this.currentGameSlug = currentGameSlug; }

    public String getCurrentRoomCode() { return currentRoomCode; }
    public void setCurrentRoomCode(String currentRoomCode) { this.currentRoomCode = currentRoomCode; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
