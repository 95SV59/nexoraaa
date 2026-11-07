package com.nexora.progression;

import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_achievement", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "achievement_id"})
})
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;

    @Column(name = "progress", nullable = false)
    private int progress = 0;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    protected UserAchievement() {}

    public UserAchievement(User user, Achievement achievement) {
        this.user = user;
        this.achievement = achievement;
        this.progress = 0;
        this.completed = false;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Achievement getAchievement() { return achievement; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }

    public void markUnlocked() {
        this.completed = true;
        this.unlockedAt = LocalDateTime.now();
    }
}
