package com.nexora.game;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps to game (docs/DATABASE_DESIGN.md). slug is what the multiplayer side already uses
 * as gameId everywhere.
 */
@Entity
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 120)
    private String title;

    @Lob
    private String description;

    @Column(length = 120)
    private String developer;

    @Column(length = 120)
    private String publisher;

    @Column(name = "is_multiplayer", nullable = false)
    private boolean multiplayer;

    @Column(name = "min_players")
    private Integer minPlayers;

    @Column(name = "max_players")
    private Integer maxPlayers;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "game_platform",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "platform_id"))
    private Set<Platform> platforms = new HashSet<>();

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Game() { }

    public Game(String slug, String title, String description, String developer, String publisher,
                boolean multiplayer, Integer minPlayers, Integer maxPlayers, Genre genre, LocalDate releaseDate) {
        this.slug = slug;
        this.title = title;
        this.description = description;
        this.developer = developer;
        this.publisher = publisher;
        this.multiplayer = multiplayer;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.genre = genre;
        this.releaseDate = releaseDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDeveloper() { return developer; }
    public void setDeveloper(String developer) { this.developer = developer; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public boolean isMultiplayer() { return multiplayer; }
    public void setMultiplayer(boolean multiplayer) { this.multiplayer = multiplayer; }
    public Integer getMinPlayers() { return minPlayers; }
    public void setMinPlayers(Integer minPlayers) { this.minPlayers = minPlayers; }
    public Integer getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(Integer maxPlayers) { this.maxPlayers = maxPlayers; }
    public Genre getGenre() { return genre; }
    public void setGenre(Genre genre) { this.genre = genre; }
    public Set<Platform> getPlatforms() { return platforms; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public Instant getCreatedAt() { return createdAt; }

    public void addPlatform(Platform platform) { platforms.add(platform); }
}
