package com.nexora.social.group;

import com.nexora.social.group.dto.CreateGroupRequest;
import com.nexora.social.group.dto.GroupDto;
import com.nexora.user.Role;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroupServiceTest {

    private CommunityGroupRepository groupRepository;
    private CommunityGroupMemberRepository memberRepository;
    private UserRepository userRepository;
    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupRepository = mock(CommunityGroupRepository.class);
        memberRepository = mock(CommunityGroupMemberRepository.class);
        userRepository = mock(UserRepository.class);

        groupService = new GroupService(groupRepository, memberRepository, userRepository);
    }

    @Test
    @DisplayName("Creates new syndicate group and assigns creator as leader")
    void testCreateGroup() {
        Long userId = 1L;
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Vanguard Ops");
        req.setTag("VG");
        req.setDescription("Elite squad");

        when(groupRepository.existsByName("Vanguard Ops")).thenReturn(false);
        when(groupRepository.existsByTag("VG")).thenReturn(false);

        CommunityGroup savedGroup = new CommunityGroup("Vanguard Ops", "VG", "Elite squad", "avatar", userId);
        savedGroup.setId(10L);
        when(groupRepository.save(any(CommunityGroup.class))).thenReturn(savedGroup);

        User leader = new User("commander", "cmd@nexora.gg", "hash", new Role("USER"));
        leader.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(leader));

        CommunityGroupMember leaderMembership = new CommunityGroupMember(savedGroup, userId, "LEADER");
        when(memberRepository.findByGroupIdAndUserId(10L, userId)).thenReturn(Optional.of(leaderMembership));
        when(memberRepository.findByGroupId(10L)).thenReturn(Collections.singletonList(leaderMembership));
        when(userRepository.findAllById(any())).thenReturn(Collections.singletonList(leader));

        GroupDto dto = groupService.createGroup(req, userId);

        assertNotNull(dto);
        assertEquals("Vanguard Ops", dto.name());
        assertEquals("VG", dto.tag());
        assertEquals("LEADER", dto.myRole());
        assertTrue(dto.isMember());
        verify(groupRepository, times(1)).save(any(CommunityGroup.class));
        verify(memberRepository, times(1)).save(any(CommunityGroupMember.class));
    }

    @Test
    @DisplayName("Player joins group and increments member count")
    void testJoinGroup() {
        Long groupId = 10L;
        Long userId = 2L;

        CommunityGroup group = new CommunityGroup("Shadow Syndicate", "SHD", "Stealth clan", "avatar", 1L);
        group.setId(groupId);
        group.setMemberCount(1);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.existsByGroupIdAndUserId(groupId, userId)).thenReturn(false);
        when(groupRepository.save(any(CommunityGroup.class))).thenReturn(group);

        User leader = new User("leader", "l@nexora.gg", "hash", new Role("USER"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(leader));

        CommunityGroupMember membership = new CommunityGroupMember(group, userId, "MEMBER");
        when(memberRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(membership));

        GroupDto result = groupService.joinGroup(groupId, userId);

        assertNotNull(result);
        assertEquals(2, group.getMemberCount());
        verify(memberRepository, times(1)).save(any(CommunityGroupMember.class));
        verify(groupRepository, times(1)).save(group);
    }

    @Test
    @DisplayName("Disallows leader from leaving clan without disbanding")
    void testLeaderCannotLeave() {
        Long groupId = 10L;
        Long leaderId = 1L;

        CommunityGroup group = new CommunityGroup("Alpha Clan", "ALP", "Top clan", "avatar", leaderId);
        group.setId(groupId);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                groupService.leaveGroup(groupId, leaderId)
        );
        assertTrue(ex.getMessage().contains("Clan leaders cannot leave their own clan"));
        verify(memberRepository, never()).deleteByGroupIdAndUserId(any(), any());
    }
}
