package com.nexora.community.dto;

import com.nexora.community.CommunityPostType;
import java.time.Instant;

public record CommunityPostDto(
        Long id,
        String gameSlug,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        String title,
        String content,
        CommunityPostType postType,
        int likesCount,
        Instant createdAt
) {}
