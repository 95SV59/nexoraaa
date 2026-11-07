package com.nexora.wishlist.dto;

import java.time.Instant;

public class WishlistItemDto {
    private Long id;
    private Long gameId;
    private String gameSlug;
    private String title;
    private String description;
    private String genreName;
    private String thumbnailUrl;
    private boolean multiplayer;
    private Instant addedAt;

    public WishlistItemDto() { }

    public WishlistItemDto(Long id, Long gameId, String gameSlug, String title, String description,
                           String genreName, String thumbnailUrl, boolean multiplayer, Instant addedAt) {
        this.id = id;
        this.gameId = gameId;
        this.gameSlug = gameSlug;
        this.title = title;
        this.description = description;
        this.genreName = genreName;
        this.thumbnailUrl = thumbnailUrl;
        this.multiplayer = multiplayer;
        this.addedAt = addedAt;
    }

    public Long getId() { return id; }
    public Long getGameId() { return gameId; }
    public String getGameSlug() { return gameSlug; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getGenreName() { return genreName; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public boolean isMultiplayer() { return multiplayer; }
    public Instant getAddedAt() { return addedAt; }
}
