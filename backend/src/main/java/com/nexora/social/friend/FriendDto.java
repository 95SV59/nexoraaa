package com.nexora.social.friend;

import com.nexora.social.presence.PresenceStatus;
import java.time.Instant;

public record FriendDto(
    Long friendshipId,
    Long friendUserId,
    String username,
    String avatarUrl,
    FriendshipStatus status,
    PresenceStatus presenceStatus,
    String currentGameSlug,
    String currentRoomCode,
    Instant memberSince
) {}
