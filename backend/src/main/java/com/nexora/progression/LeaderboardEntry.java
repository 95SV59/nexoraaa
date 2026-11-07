package com.nexora.progression;

import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"game_slug", "user_id", "season"})
})
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_slug", nullable = false, length = 60)
    private String gameSlug;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "score", nullable = false)
    private int score = 1000;

    @Column(name = "rank_tier", nullable = false, length = 30)
    private String rankTier = "BRONZE";

    @Column(name = "matches_played", nullable = false)
    private int matchesPlayed = 0;

    @Column(name = "wins", nullable = false)
    private int wins = 0;

    @Column(name = "season", nullable = false)
    private int season = 1;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected LeaderboardEntry() {}

    public LeaderboardEntry(String gameSlug, User user, int season) {
        this.gameSlug = gameSlug;
        this.user = user;
        this.season = season;
        this.score = 1000;
        this.rankTier = "BRONZE";
        this.matchesPlayed = 0;
        this.wins = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getGameSlug() { return gameSlug; }
    public User getUser() { return user; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getRankTier() { return rankTier; }
    public void setRankTier(String rankTier) { this.rankTier = rankTier; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getSeason() { return season; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
