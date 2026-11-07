package com.nexora.progression;

import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progression")
public class UserProgression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_xp", nullable = false)
    private int totalXp = 0;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "rank_title", nullable = false, length = 60)
    private String rankTitle = "Rookie Operative";

    @Column(name = "season_pass_tier", nullable = false)
    private int seasonPassTier = 1;

    @Column(name = "matches_played", nullable = false)
    private int matchesPlayed = 0;

    @Column(name = "matches_won", nullable = false)
    private int matchesWon = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected UserProgression() {}

    public UserProgression(User user) {
        this.user = user;
        this.totalXp = 0;
        this.level = 1;
        this.rankTitle = "Rookie Operative";
        this.seasonPassTier = 1;
        this.matchesPlayed = 0;
        this.matchesWon = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getRankTitle() { return rankTitle; }
    public void setRankTitle(String rankTitle) { this.rankTitle = rankTitle; }
    public int getSeasonPassTier() { return seasonPassTier; }
    public void setSeasonPassTier(int seasonPassTier) { this.seasonPassTier = seasonPassTier; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }
    public int getMatchesWon() { return matchesWon; }
    public void setMatchesWon(int matchesWon) { this.matchesWon = matchesWon; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
