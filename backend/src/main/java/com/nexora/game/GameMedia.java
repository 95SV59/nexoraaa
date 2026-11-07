package com.nexora.game;

import jakarta.persistence.*;

/** Maps to game_media (docs/DATABASE_DESIGN.md). */
@Entity
@Table(name = "game_media")
public class GameMedia {

    public enum MediaType { SCREENSHOT, TRAILER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaType mediaType;

    @Column(nullable = false)
    private String url;

    protected GameMedia() { }

    public GameMedia(Game game, MediaType mediaType, String url) {
        this.game = game;
        this.mediaType = mediaType;
        this.url = url;
    }

    public Long getId() { return id; }
    public MediaType getMediaType() { return mediaType; }
    public String getUrl() { return url; }
}
