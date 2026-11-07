package com.nexora.social.friend;

import com.nexora.social.notification.NotificationService;
import com.nexora.social.notification.NotificationType;
import com.nexora.social.presence.PresenceDto;
import com.nexora.social.presence.PresenceService;
import com.nexora.social.presence.PresenceStatus;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final NotificationService notificationService;

    public FriendService(FriendshipRepository friendshipRepository,
                         UserRepository userRepository,
                         PresenceService presenceService,
                         NotificationService notificationService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
        this.notificationService = notificationService;
    }

    @Transactional
    public FriendDto sendFriendRequest(Long senderUserId, String targetUsername) {
        User sender = userRepository.findById(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + senderUserId));

        User target = userRepository.findByUsername(targetUsername.trim())
                .orElseThrow(() -> new IllegalArgumentException("Target player not found: " + targetUsername));

        if (sender.getId().equals(target.getId())) {
            throw new IllegalArgumentException("You cannot add yourself as a friend.");
        }

        Optional<Friendship> existing = friendshipRepository.findRelation(sender.getId(), target.getId());
        Friendship friendship;
        if (existing.isPresent()) {
            friendship = existing.get();
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new IllegalArgumentException("You are already friends with " + target.getUsername());
            }
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                throw new IllegalArgumentException("Unable to send friend request.");
            }
            // Update direction if re-requesting after decline
            friendship.setUser(sender);
            friendship.setFriend(target);
            friendship.setStatus(FriendshipStatus.PENDING);
        } else {
            friendship = new Friendship(sender, target, FriendshipStatus.PENDING);
        }
        Friendship saved = friendshipRepository.save(friendship);

        // Alert target player via notification
        notificationService.sendNotification(
                target.getId(),
                NotificationType.FRIEND_REQUEST,
                "New Friend Request",
                sender.getUsername() + " sent you a friend request.",
                "/profile"
        );

        return toDto(saved, senderUserId);
    }

    @Transactional
    public FriendDto acceptFriendRequest(Long currentUserId, Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found: " + friendshipId));

        if (!f.getFriend().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You can only accept requests sent to you.");
        }

        f.setStatus(FriendshipStatus.ACCEPTED);
        Friendship saved = friendshipRepository.save(f);

        // Alert sender that their request was accepted
        notificationService.sendNotification(
                f.getUser().getId(),
                NotificationType.FRIEND_ACCEPT,
                "Friend Request Accepted",
                f.getFriend().getUsername() + " accepted your friend request.",
                "/profile"
        );

        return toDto(saved, currentUserId);
    }

    @Transactional
    public void declineFriendRequest(Long currentUserId, Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found: " + friendshipId));

        if (!f.getFriend().getId().equals(currentUserId) && !f.getUser().getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Unauthorized to decline this request.");
        }

        f.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(f);
    }

    @Transactional
    public void removeFriend(Long currentUserId, Long friendUserId) {
        friendshipRepository.findRelation(currentUserId, friendUserId)
                .ifPresent(friendshipRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getFriends(Long currentUserId) {
        return friendshipRepository.findAllAcceptedFriends(currentUserId).stream()
                .map(f -> toDto(f, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getIncomingPendingRequests(Long currentUserId) {
        return friendshipRepository.findPendingIncomingRequests(currentUserId).stream()
                .map(f -> toDto(f, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getOutgoingPendingRequests(Long currentUserId) {
        return friendshipRepository.findPendingOutgoingRequests(currentUserId).stream()
                .map(f -> toDto(f, currentUserId))
                .collect(Collectors.toList());
    }

    private FriendDto toDto(Friendship f, Long currentUserId) {
        // The other person in this friendship
        User other = f.getUser().getId().equals(currentUserId) ? f.getFriend() : f.getUser();
        PresenceDto presence = presenceService.getPresence(other.getId());

        return new FriendDto(
                f.getId(),
                other.getId(),
                other.getUsername(),
                other.getAvatarUrl(),
                f.getStatus(),
                presence.status() != null ? presence.status() : PresenceStatus.OFFLINE,
                presence.currentGameSlug(),
                presence.currentRoomCode(),
                other.getCreatedAt()
        );
    }
}
