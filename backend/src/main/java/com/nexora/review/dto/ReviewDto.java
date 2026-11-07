package com.nexora.review.dto;

import java.time.Instant;

public class ReviewDto {
    private Long id;
    private Long gameId;
    private String gameSlug;
    private String gameTitle;
    private Long userId;
    private String username;
    private String userAvatarUrl;
    private int rating;
    private boolean recommended;
    private Double hoursPlayed;
    private boolean spoiler;
    private String body;
    private boolean reported;
    private long likeCount;
    private boolean likedByCurrentUser;
    private Instant createdAt;

    public ReviewDto() { }

    public ReviewDto(Long id, Long gameId, String gameSlug, String gameTitle, Long userId,
                     String username, String userAvatarUrl, int rating, boolean recommended,
                     Double hoursPlayed, boolean spoiler, String body,
                     boolean reported, long likeCount, boolean likedByCurrentUser, Instant createdAt) {
        this.id = id;
        this.gameId = gameId;
        this.gameSlug = gameSlug;
        this.gameTitle = gameTitle;
        this.userId = userId;
        this.username = username;
        this.userAvatarUrl = userAvatarUrl;
        this.rating = rating;
        this.recommended = recommended;
        this.hoursPlayed = hoursPlayed;
        this.spoiler = spoiler;
        this.body = body;
        this.reported = reported;
        this.likeCount = likeCount;
        this.likedByCurrentUser = likedByCurrentUser;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getGameId() { return gameId; }
    public String getGameSlug() { return gameSlug; }
    public String getGameTitle() { return gameTitle; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getUserAvatarUrl() { return userAvatarUrl; }
    public int getRating() { return rating; }
    public boolean isRecommended() { return recommended; }
    public Double getHoursPlayed() { return hoursPlayed != null ? hoursPlayed : 0.0; }
    public boolean isSpoiler() { return spoiler; }
    public String getBody() { return body; }
    public boolean isReported() { return reported; }
    public long getLikeCount() { return likeCount; }
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; }
    public Instant getCreatedAt() { return createdAt; }
}
