package com.nexora.social.group;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommunityGroupMemberRepository extends JpaRepository<CommunityGroupMember, Long> {
    List<CommunityGroupMember> findByGroupId(Long groupId);
    List<CommunityGroupMember> findByUserId(Long userId);
    Optional<CommunityGroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    void deleteByGroupIdAndUserId(Long groupId, Long userId);
}
