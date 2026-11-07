package com.nexora.social.presence;

import java.time.LocalDateTime;

public record PresenceDto(
    Long userId,
    String username,
    String avatarUrl,
    PresenceStatus status,
    String currentGameSlug,
    String currentRoomCode,
    LocalDateTime lastSeen
) {}
