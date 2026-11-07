package com.nexora.multiplayer.party;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyInvitationRepository extends JpaRepository<PartyInvitation, Long> {
    List<PartyInvitation> findByInviteeIdAndStatus(Long inviteeId, PartyInvitation.Status status);
}
