package com.nexora.social.group;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_group")
public class CommunityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "tag", nullable = false, unique = true, length = 10)
    private String tag;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "leader_user_id", nullable = false)
    private Long leaderUserId;

    @Column(name = "member_count", nullable = false)
    private int memberCount = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected CommunityGroup() {}

    public CommunityGroup(String name, String tag, String description, String avatarUrl, Long leaderUserId) {
        this.name = name;
        this.tag = tag;
        this.description = description;
        this.avatarUrl = avatarUrl;
        this.leaderUserId = leaderUserId;
        this.memberCount = 1;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Long getLeaderUserId() { return leaderUserId; }
    public void setLeaderUserId(Long leaderUserId) { this.leaderUserId = leaderUserId; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public Instant getCreatedAt() { return createdAt; }
}
