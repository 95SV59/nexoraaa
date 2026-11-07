package com.nexora.progression.dto;

public record LeaderboardEntryDto(
    int rank,
    Long userId,
    String username,
    String avatarUrl,
    String gameSlug,
    int score,
    String rankTier,
    int matchesPlayed,
    int wins,
    double winRatePct,
    int season
) {}
