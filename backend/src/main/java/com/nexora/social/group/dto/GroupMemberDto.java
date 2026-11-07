package com.nexora.social.group.dto;

import java.time.Instant;

public record GroupMemberDto(
        Long userId,
        String username,
        String avatarUrl,
        String role,
        Instant joinedAt
) {}
