package com.nexora.multiplayer.party;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyMemberRepository extends JpaRepository<PartyMember, Long> {
    Optional<PartyMember> findByUserId(Long userId);
    List<PartyMember> findByPartyId(Long partyId);
    void deleteByUserId(Long userId);
}
