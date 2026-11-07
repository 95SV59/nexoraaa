package com.nexora.library.dto;

import com.nexora.library.LibraryStatus;
import java.time.Instant;

public class LibraryItemDto {
    private Long id;
    private Long gameId;
    private String gameSlug;
    private String title;
    private String description;
    private String genreName;
    private String thumbnailUrl;
    private boolean multiplayer;
    private LibraryStatus status;
    private Double hoursPlayed;
    private Instant lastPlayed;
    private boolean isFavorite;
    private String userNotes;
    private Instant updatedAt;

    public LibraryItemDto() { }

    public LibraryItemDto(Long id, Long gameId, String gameSlug, String title, String description,
                          String genreName, String thumbnailUrl, boolean multiplayer,
                          LibraryStatus status, Double hoursPlayed, Instant lastPlayed,
                          boolean isFavorite, String userNotes, Instant updatedAt) {
        this.id = id;
        this.gameId = gameId;
        this.gameSlug = gameSlug;
        this.title = title;
        this.description = description;
        this.genreName = genreName;
        this.thumbnailUrl = thumbnailUrl;
        this.multiplayer = multiplayer;
        this.status = status;
        this.hoursPlayed = hoursPlayed;
        this.lastPlayed = lastPlayed;
        this.isFavorite = isFavorite;
        this.userNotes = userNotes;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getGameId() { return gameId; }
    public String getGameSlug() { return gameSlug; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getGenreName() { return genreName; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public boolean isMultiplayer() { return multiplayer; }
    public LibraryStatus getStatus() { return status; }
    public Double getHoursPlayed() { return hoursPlayed != null ? hoursPlayed : 0.0; }
    public Instant getLastPlayed() { return lastPlayed; }
    public boolean isFavorite() { return isFavorite; }
    public String getUserNotes() { return userNotes; }
    public Instant getUpdatedAt() { return updatedAt; }
}
