package com.nexora.article;

import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private ArticleCategory category;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "is_published", nullable = false)
    private boolean published = false;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected Article() { }

    public Article(String title, String body, ArticleCategory category, User author, boolean published) {
        this.title = title;
        this.body = body;
        this.category = category;
        this.author = author;
        this.published = published;
        if (published) {
            this.publishedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public ArticleCategory getCategory() { return category; }
    public void setCategory(ArticleCategory category) { this.category = category; }
    public User getAuthor() { return author; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) {
        this.published = published;
        if (published && this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
    }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
