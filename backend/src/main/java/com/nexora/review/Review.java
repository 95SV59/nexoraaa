package com.nexora.review;

import com.nexora.game.Game;
import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private int rating; // 1-5

    @Column(name = "is_recommended", nullable = false)
    private boolean recommended = true;

    @Column(name = "hours_played", nullable = false)
    private Double hoursPlayed = 0.0;

    @Column(name = "is_spoiler", nullable = false)
    private boolean spoiler = false;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_reported", nullable = false)
    private boolean reported = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Review() { }

    public Review(User user, Game game, int rating, String body, boolean recommended, double hoursPlayed, boolean spoiler) {
        this.user = user;
        this.game = game;
        this.rating = rating;
        this.body = body;
        this.recommended = recommended;
        this.hoursPlayed = hoursPlayed;
        this.spoiler = spoiler;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public Game getGame() { return game; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public boolean isRecommended() { return recommended; }
    public void setRecommended(boolean recommended) { this.recommended = recommended; }
    public Double getHoursPlayed() { return hoursPlayed != null ? hoursPlayed : 0.0; }
    public void setHoursPlayed(Double hoursPlayed) { this.hoursPlayed = hoursPlayed; }
    public boolean isSpoiler() { return spoiler; }
    public void setSpoiler(boolean spoiler) { this.spoiler = spoiler; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isReported() { return reported; }
    public void setReported(boolean reported) { this.reported = reported; }
    public Instant getCreatedAt() { return createdAt; }
}
