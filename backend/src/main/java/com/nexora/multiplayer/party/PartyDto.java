package com.nexora.multiplayer.party;

import java.time.LocalDateTime;
import java.util.List;

public record PartyDto(
    Long id,
    String partyCode,
    Long leaderUserId,
    String leaderUsername,
    int maxMembers,
    List<PartyMemberDto> members,
    LocalDateTime createdAt
) {
    public record PartyMemberDto(
        Long userId,
        String username,
        String avatarUrl,
        boolean isLeader,
        LocalDateTime joinedAt
    ) {}
}
