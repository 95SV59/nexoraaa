package com.nexora.admin.dto;

public class AdminStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long totalGames;
    private long reportedReviewsCount;
    private long totalArticles;
    private long totalSessions;
    private long activeRooms;

    public AdminStatsDto() { }

    public AdminStatsDto(long totalUsers, long activeUsers, long totalGames, long reportedReviewsCount,
                         long totalArticles, long totalSessions, long activeRooms) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.totalGames = totalGames;
        this.reportedReviewsCount = reportedReviewsCount;
        this.totalArticles = totalArticles;
        this.totalSessions = totalSessions;
        this.activeRooms = activeRooms;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getActiveUsers() { return activeUsers; }
    public long getTotalGames() { return totalGames; }
    public long getReportedReviewsCount() { return reportedReviewsCount; }
    public long getTotalArticles() { return totalArticles; }
    public long getTotalSessions() { return totalSessions; }
    public long getActiveRooms() { return activeRooms; }
}
