package com.nexora.library;

import com.nexora.game.Game;
import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_library", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "game_id"})
})
public class Library {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LibraryStatus status;

    @Column(name = "hours_played", nullable = false)
    private Double hoursPlayed = 0.0;

    @Column(name = "last_played")
    private Instant lastPlayed;

    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite = false;

    @Column(name = "user_notes", columnDefinition = "TEXT")
    private String userNotes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Library() { }

    public Library(User user, Game game, LibraryStatus status) {
        this.user = user;
        this.game = game;
        this.status = status;
        this.hoursPlayed = 0.0;
        this.isFavorite = false;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public Game getGame() { return game; }
    public LibraryStatus getStatus() { return status; }
    public void setStatus(LibraryStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Double getHoursPlayed() { return hoursPlayed != null ? hoursPlayed : 0.0; }
    public void setHoursPlayed(Double hoursPlayed) { this.hoursPlayed = hoursPlayed; }
    public void addPlaytime(double hours) {
        this.hoursPlayed = (this.hoursPlayed != null ? this.hoursPlayed : 0.0) + Math.max(0.0, hours);
        this.lastPlayed = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Instant getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(Instant lastPlayed) { this.lastPlayed = lastPlayed; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) {
        this.isFavorite = favorite;
        this.updatedAt = Instant.now();
    }

    public String getUserNotes() { return userNotes; }
    public void setUserNotes(String userNotes) {
        this.userNotes = userNotes;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() { return updatedAt; }
}
