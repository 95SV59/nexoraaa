package com.nexora.social.group;

import com.nexora.social.group.dto.CreateGroupRequest;
import com.nexora.social.group.dto.GroupDto;
import com.nexora.social.group.dto.GroupMemberDto;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final CommunityGroupRepository groupRepository;
    private final CommunityGroupMemberRepository memberRepository;
    private final UserRepository userRepository;

    public GroupService(CommunityGroupRepository groupRepository,
                        CommunityGroupMemberRepository memberRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupDto createGroup(CreateGroupRequest req, Long userId) {
        if (groupRepository.existsByName(req.getName())) {
            throw new IllegalArgumentException("A group with this name already exists.");
        }
        if (groupRepository.existsByTag(req.getTag().toUpperCase())) {
            throw new IllegalArgumentException("A clan tag with this code already exists.");
        }

        CommunityGroup group = new CommunityGroup(
                req.getName(),
                req.getTag().toUpperCase(),
                req.getDescription(),
                req.getAvatarUrl() != null && !req.getAvatarUrl().isBlank()
                        ? req.getAvatarUrl()
                        : "https://api.dicebear.com/7.x/bottts/svg?seed=" + req.getName(),
                userId
        );
        group = groupRepository.save(group);

        CommunityGroupMember leaderMember = new CommunityGroupMember(group, userId, "LEADER");
        memberRepository.save(leaderMember);

        return toDto(group, userId, true);
    }

    public List<GroupDto> listAllGroups(Long currentUserId) {
        return groupRepository.findAll().stream()
                .map(g -> toDto(g, currentUserId, false))
                .collect(Collectors.toList());
    }

    public List<GroupDto> listMyGroups(Long currentUserId) {
        List<CommunityGroupMember> memberships = memberRepository.findByUserId(currentUserId);
        return memberships.stream()
                .map(m -> toDto(m.getGroup(), currentUserId, false))
                .collect(Collectors.toList());
    }

    public GroupDto getGroupDetails(Long groupId, Long currentUserId) {
        CommunityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));
        return toDto(group, currentUserId, true);
    }

    @Transactional
    public GroupDto joinGroup(Long groupId, Long userId) {
        CommunityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        if (memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalStateException("Already a member of this clan.");
        }

        CommunityGroupMember member = new CommunityGroupMember(group, userId, "MEMBER");
        memberRepository.save(member);

        group.setMemberCount(group.getMemberCount() + 1);
        groupRepository.save(group);

        return toDto(group, userId, true);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        CommunityGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found with id: " + groupId));

        if (group.getLeaderUserId().equals(userId)) {
            throw new IllegalStateException("Clan leaders cannot leave their own clan. Transfer leadership or disband.");
        }

        memberRepository.deleteByGroupIdAndUserId(groupId, userId);
        group.setMemberCount(Math.max(1, group.getMemberCount() - 1));
        groupRepository.save(group);
    }

    private GroupDto toDto(CommunityGroup group, Long currentUserId, boolean includeMembers) {
        Optional<CommunityGroupMember> myMembership = memberRepository.findByGroupIdAndUserId(group.getId(), currentUserId);
        boolean isMember = myMembership.isPresent();
        String myRole = myMembership.map(CommunityGroupMember::getRole).orElse(null);

        String leaderUsername = userRepository.findById(group.getLeaderUserId())
                .map(User::getUsername)
                .orElse("Operative");

        List<GroupMemberDto> memberDtos = null;
        if (includeMembers) {
            List<CommunityGroupMember> members = memberRepository.findByGroupId(group.getId());
            List<Long> userIds = members.stream().map(CommunityGroupMember::getUserId).toList();
            Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            memberDtos = members.stream().map(m -> {
                User u = userMap.get(m.getUserId());
                return new GroupMemberDto(
                        m.getUserId(),
                        u != null ? u.getUsername() : "Operative",
                        u != null ? u.getAvatarUrl() : null,
                        m.getRole(),
                        m.getJoinedAt()
                );
            }).collect(Collectors.toList());
        }

        return new GroupDto(
                group.getId(),
                group.getName(),
                group.getTag(),
                group.getDescription(),
                group.getAvatarUrl(),
                group.getLeaderUserId(),
                leaderUsername,
                group.getMemberCount(),
                isMember,
                myRole,
                group.getCreatedAt(),
                memberDtos
        );
    }
}
