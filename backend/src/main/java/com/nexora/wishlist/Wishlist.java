package com.nexora.wishlist;

import com.nexora.game.Game;
import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "wishlist", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "game_id"})
})
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt = Instant.now();

    protected Wishlist() { }

    public Wishlist(User user, Game game) {
        this.user = user;
        this.game = game;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Game getGame() { return game; }
    public Instant getAddedAt() { return addedAt; }
}
