package com.nexora.community.dto;

import com.nexora.community.CommunityPostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityPostRequest(
        @NotBlank(message = "Title cannot be empty")
        @Size(max = 200, message = "Title is too long")
        String title,

        @NotBlank(message = "Content cannot be empty")
        String content,

        CommunityPostType postType
) {}
