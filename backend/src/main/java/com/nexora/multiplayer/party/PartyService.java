package com.nexora.multiplayer.party;

import com.nexora.multiplayer.room.GameRoom;
import com.nexora.multiplayer.room.RoomService;
import com.nexora.social.notification.NotificationService;
import com.nexora.social.notification.NotificationType;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PartyService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final PartyRepository partyRepository;
    private final PartyMemberRepository memberRepository;
    private final PartyInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoomService roomService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public PartyService(PartyRepository partyRepository,
                        PartyMemberRepository memberRepository,
                        PartyInvitationRepository invitationRepository,
                        UserRepository userRepository,
                        RoomService roomService,
                        NotificationService notificationService,
                        SimpMessagingTemplate messagingTemplate) {
        this.partyRepository = partyRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.roomService = roomService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public PartyDto createParty(Long leaderUserId) {
        // Leave any existing party
        leaveParty(leaderUserId);

        User leader = userRepository.findById(leaderUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + leaderUserId));

        String code = generatePartyCode();
        Party party = new Party(code, leader, 4);
        Party savedParty = partyRepository.save(party);

        PartyMember leaderMember = new PartyMember(savedParty, leader);
        memberRepository.save(leaderMember);

        return toDto(savedParty);
    }

    @Transactional
    public void inviteToParty(Long inviterUserId, Long inviteeUserId) {
        PartyMember inviterMember = memberRepository.findByUserId(inviterUserId)
                .orElseThrow(() -> new IllegalArgumentException("You are not currently in a party."));

        Party party = inviterMember.getParty();
        if (party.getMembers().size() >= party.getMaxMembers()) {
            throw new IllegalArgumentException("Party is already full.");
        }

        User inviter = inviterMember.getUser();
        User invitee = userRepository.findById(inviteeUserId)
                .orElseThrow(() -> new IllegalArgumentException("Invitee not found: " + inviteeUserId));

        PartyInvitation inv = new PartyInvitation(party, inviter, invitee);
        invitationRepository.save(inv);

        notificationService.sendNotification(
                invitee.getId(),
                NotificationType.PARTY_INVITE,
                "Party Invitation",
                inviter.getUsername() + " invited you to their party (" + party.getPartyCode() + ").",
                "/play/arena"
        );
    }

    @Transactional
    public PartyDto respondInvitation(Long invitationId, Long inviteeUserId, boolean accept) {
        PartyInvitation inv = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found: " + invitationId));

        if (!inv.getInvitee().getId().equals(inviteeUserId)) {
            throw new IllegalArgumentException("Unauthorized to respond to this invitation.");
        }

        if (accept) {
            leaveParty(inviteeUserId);
            inv.setStatus(PartyInvitation.Status.ACCEPTED);
            Party party = inv.getParty();
            if (party.getMembers().size() >= party.getMaxMembers()) {
                throw new IllegalArgumentException("Party has become full.");
            }
            PartyMember member = new PartyMember(party, inv.getInvitee());
            memberRepository.save(member);
            broadcastPartyUpdate(party);
            return toDto(party);
        } else {
            inv.setStatus(PartyInvitation.Status.DECLINED);
            invitationRepository.save(inv);
            return null;
        }
    }

    @Transactional
    public void leaveParty(Long userId) {
        Optional<PartyMember> memberOpt = memberRepository.findByUserId(userId);
        if (memberOpt.isPresent()) {
            PartyMember member = memberOpt.get();
            Party party = member.getParty();
            memberRepository.delete(member);

            List<PartyMember> remaining = memberRepository.findByPartyId(party.getId());
            if (remaining.isEmpty()) {
                partyRepository.delete(party);
            } else if (party.getLeader().getId().equals(userId)) {
                // Promote next member to leader
                party.setLeader(remaining.get(0).getUser());
                partyRepository.save(party);
                broadcastPartyUpdate(party);
            } else {
                broadcastPartyUpdate(party);
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<PartyDto> getCurrentParty(Long userId) {
        return memberRepository.findByUserId(userId)
                .map(pm -> toDto(pm.getParty()));
    }

    @Transactional
    public String launchPartyMatch(Long leaderUserId, String gameSlug) {
        PartyMember leaderMember = memberRepository.findByUserId(leaderUserId)
                .orElseThrow(() -> new IllegalArgumentException("You are not in a party."));

        Party party = leaderMember.getParty();
        if (!party.getLeader().getId().equals(leaderUserId)) {
            throw new IllegalArgumentException("Only the party leader can launch a match.");
        }

        List<PartyMember> members = memberRepository.findByPartyId(party.getId());

        // Create Room
        GameRoom room = roomService.createRoom(gameSlug, 8, "US-EAST", leaderUserId, party.getLeader().getUsername());

        // Join other members into the room
        for (PartyMember m : members) {
            if (!m.getUser().getId().equals(leaderUserId)) {
                roomService.joinRoom(room.getRoomCode(), m.getUser().getId(), m.getUser().getUsername());
            }
        }

        // Broadcast to all party members via STOMP
        messagingTemplate.convertAndSend("/topic/party/" + party.getPartyCode() + "/launch",
                java.util.Map.of("roomCode", room.getRoomCode(), "gameSlug", gameSlug));

        return room.getRoomCode();
    }

    private void broadcastPartyUpdate(Party party) {
        PartyDto dto = toDto(party);
        messagingTemplate.convertAndSend("/topic/party/" + party.getPartyCode(), dto);
    }

    private PartyDto toDto(Party party) {
        List<PartyMember> members = memberRepository.findByPartyId(party.getId());
        List<PartyDto.PartyMemberDto> memberDtos = members.stream()
                .map(m -> new PartyDto.PartyMemberDto(
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        m.getUser().getAvatarUrl(),
                        m.getUser().getId().equals(party.getLeader().getId()),
                        m.getJoinedAt()
                ))
                .collect(Collectors.toList());

        return new PartyDto(
                party.getId(),
                party.getPartyCode(),
                party.getLeader().getId(),
                party.getLeader().getUsername(),
                party.getMaxMembers(),
                memberDtos,
                party.getCreatedAt()
        );
    }

    private String generatePartyCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
