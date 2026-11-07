package com.nexora.progression.dto;

public record UserProgressionDto(
    Long userId,
    String username,
    int totalXp,
    int level,
    String rankTitle,
    int seasonPassTier,
    int matchesPlayed,
    int matchesWon,
    double winRatePct,
    int currentLevelXpFloor,
    int nextLevelXpThreshold,
    double levelProgressPct,
    long completedAchievementsCount,
    long totalAchievementsCount
) {}
