package com.nexora.multiplayer.party;

import com.nexora.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "party_member", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"party_id", "user_id"})
})
public class PartyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    public PartyMember() {}

    public PartyMember(Party party, User user) {
        this.party = party;
        this.user = user;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Party getParty() { return party; }
    public User getUser() { return user; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
