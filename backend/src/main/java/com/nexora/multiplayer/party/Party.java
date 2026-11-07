package com.nexora.multiplayer.party;

import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "party")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_code", nullable = false, unique = true, length = 10)
    private String partyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_user_id", nullable = false)
    private User leader;

    @Column(name = "max_members", nullable = false)
    private int maxMembers = 4;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyMember> members = new ArrayList<>();

    public Party() {}

    public Party(String partyCode, User leader, int maxMembers) {
        this.partyCode = partyCode;
        this.leader = leader;
        this.maxMembers = maxMembers;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getPartyCode() { return partyCode; }
    public User getLeader() { return leader; }
    public void setLeader(User leader) { this.leader = leader; }
    public int getMaxMembers() { return maxMembers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<PartyMember> getMembers() { return members; }
}
