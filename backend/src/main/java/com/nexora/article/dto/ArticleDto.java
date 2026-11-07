package com.nexora.article.dto;

import java.time.Instant;

public class ArticleDto {
    private Long id;
    private String title;
    private String body;
    private String summary;
    private Long categoryId;
    private String categoryName;
    private Long authorId;
    private String authorName;
    private boolean published;
    private Instant publishedAt;

    public ArticleDto() { }

    public ArticleDto(Long id, String title, String body, String summary, Long categoryId,
                      String categoryName, Long authorId, String authorName,
                      boolean published, Instant publishedAt) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.summary = summary;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.authorId = authorId;
        this.authorName = authorName;
        this.published = published;
        this.publishedAt = publishedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getSummary() { return summary; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public boolean isPublished() { return published; }
    public Instant getPublishedAt() { return publishedAt; }
}
