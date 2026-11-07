package com.nexora.user.dto;

import java.time.Instant;

public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;
    private String status;
    private Instant createdAt;

    // Gamer Statistics
    private long gamesInLibrary;
    private long wishlistCount;
    private long reviewsCount;
    private long matchesPlayed;
    private long wins;
    private double winRatePct;

    public UserProfileDto() { }

    public UserProfileDto(Long id, String username, String email, String role, String avatarUrl,
                          String status, Instant createdAt, long gamesInLibrary, long wishlistCount,
                          long reviewsCount, long matchesPlayed, long wins, double winRatePct) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.gamesInLibrary = gamesInLibrary;
        this.wishlistCount = wishlistCount;
        this.reviewsCount = reviewsCount;
        this.matchesPlayed = matchesPlayed;
        this.wins = wins;
        this.winRatePct = winRatePct;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public long getGamesInLibrary() { return gamesInLibrary; }
    public long getWishlistCount() { return wishlistCount; }
    public long getReviewsCount() { return reviewsCount; }
    public long getMatchesPlayed() { return matchesPlayed; }
    public long getWins() { return wins; }
    public double getWinRatePct() { return winRatePct; }
}
