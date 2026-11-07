package com.nexora.community;

import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_post")
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_slug", nullable = false, length = 60)
    private String gameSlug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 30)
    private CommunityPostType postType = CommunityPostType.DISCUSSION;

    @Column(name = "likes_count", nullable = false)
    private int likesCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected CommunityPost() { }

    public CommunityPost(String gameSlug, User user, String title, String content, CommunityPostType postType) {
        this.gameSlug = gameSlug;
        this.user = user;
        this.title = title;
        this.content = content;
        this.postType = postType != null ? postType : CommunityPostType.DISCUSSION;
        this.likesCount = 0;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getGameSlug() { return gameSlug; }
    public User getUser() { return user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public CommunityPostType getPostType() { return postType; }
    public void setPostType(CommunityPostType postType) { this.postType = postType; }
    public int getLikesCount() { return likesCount; }
    public void incrementLikes() { this.likesCount++; }
    public Instant getCreatedAt() { return createdAt; }
}
