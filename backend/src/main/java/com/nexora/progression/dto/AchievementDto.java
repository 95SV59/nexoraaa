package com.nexora.progression.dto;

import java.time.LocalDateTime;

public record AchievementDto(
    Long id,
    String gameSlug,
    String slug,
    String title,
    String description,
    String badgeIcon,
    String iconUrl,
    int xpReward,
    String criteriaType,
    int criteriaThreshold,
    String rarityTier,
    boolean completed,
    int progress,
    LocalDateTime unlockedAt
) {}
