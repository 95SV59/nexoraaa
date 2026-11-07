package com.nexora.social.group;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "community_group_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"group_id", "user_id"})
})
public class CommunityGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private CommunityGroup group;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role; // LEADER, OFFICER, MEMBER

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    protected CommunityGroupMember() {}

    public CommunityGroupMember(CommunityGroup group, Long userId, String role) {
        this.group = group;
        this.userId = userId;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public Long getId() { return id; }
    public CommunityGroup getGroup() { return group; }
    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Instant getJoinedAt() { return joinedAt; }
}
