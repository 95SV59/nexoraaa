package com.nexora.social.group.dto;

import java.time.Instant;
import java.util.List;

public record GroupDto(
        Long id,
        String name,
        String tag,
        String description,
        String avatarUrl,
        Long leaderUserId,
        String leaderUsername,
        int memberCount,
        boolean isMember,
        String myRole,
        Instant createdAt,
        List<GroupMemberDto> members
) {}
