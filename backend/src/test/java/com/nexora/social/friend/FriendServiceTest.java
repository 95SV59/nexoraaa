package com.nexora.social.friend;

import com.nexora.social.notification.NotificationService;
import com.nexora.social.notification.NotificationType;
import com.nexora.social.presence.PresenceDto;
import com.nexora.social.presence.PresenceService;
import com.nexora.social.presence.PresenceStatus;
import com.nexora.user.Role;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Platform Social Friend Service Unit Tests")
class FriendServiceTest {

    private FriendshipRepository friendshipRepository;
    private UserRepository userRepository;
    private PresenceService presenceService;
    private NotificationService notificationService;

    private FriendService friendService;

    @BeforeEach
    void setUp() {
        friendshipRepository = mock(FriendshipRepository.class);
        userRepository = mock(UserRepository.class);
        presenceService = mock(PresenceService.class);
        notificationService = mock(NotificationService.class);

        friendService = new FriendService(friendshipRepository, userRepository, presenceService, notificationService);
    }

    @Test
    @DisplayName("Cannot send friend request to oneself")
    void testCannotAddSelf() {
        Long userId = 1L;
        User user = new User("self", "self@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("self")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                friendService.sendFriendRequest(userId, "self")
        );
        assertTrue(ex.getMessage().contains("You cannot add yourself"));
    }

    @Test
    @DisplayName("Sends friend request and notifies target player")
    void testSendFriendRequestSuccess() {
        Long senderId = 1L;
        Long targetId = 2L;

        User sender = new User("alice", "alice@nexora.gg", "hash", new Role("USER"));
        sender.setId(senderId);

        User target = new User("bob", "bob@nexora.gg", "hash", new Role("USER"));
        target.setId(targetId);

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
        when(friendshipRepository.findRelation(senderId, targetId)).thenReturn(Optional.empty());

        Friendship created = new Friendship(sender, target, FriendshipStatus.PENDING);
        created.setId(99L);
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(created);
        when(presenceService.getPresence(targetId)).thenReturn(new PresenceDto(targetId, "bob", null, PresenceStatus.ONLINE, null, null, java.time.LocalDateTime.now()));

        FriendDto result = friendService.sendFriendRequest(senderId, "bob");

        assertNotNull(result);
        assertEquals("bob", result.username());
        assertEquals(FriendshipStatus.PENDING, result.status());

        // Verify notification dispatched to target player
        verify(notificationService, times(1)).sendNotification(
                eq(targetId),
                eq(NotificationType.FRIEND_REQUEST),
                contains("Friend Request"),
                contains("alice"),
                any()
        );
    }

    @Test
    @DisplayName("Accepts pending friend request and transitions status to ACCEPTED")
    void testAcceptFriendRequest() {
        Long receiverId = 2L;
        Long senderId = 1L;

        User sender = new User("alice", "alice@nexora.gg", "hash", new Role("USER"));
        sender.setId(senderId);
        User receiver = new User("bob", "bob@nexora.gg", "hash", new Role("USER"));
        receiver.setId(receiverId);

        Friendship pending = new Friendship(sender, receiver, FriendshipStatus.PENDING);
        pending.setId(77L);

        when(friendshipRepository.findById(77L)).thenReturn(Optional.of(pending));
        when(friendshipRepository.save(any(Friendship.class))).thenReturn(pending);
        when(presenceService.getPresence(senderId)).thenReturn(new PresenceDto(senderId, "alice", null, PresenceStatus.ONLINE, null, null, java.time.LocalDateTime.now()));

        FriendDto accepted = friendService.acceptFriendRequest(receiverId, 77L);

        assertNotNull(accepted);
        assertEquals(FriendshipStatus.ACCEPTED, pending.getStatus());
        verify(notificationService, times(1)).sendNotification(
                eq(senderId),
                eq(NotificationType.FRIEND_ACCEPT),
                contains("Accepted"),
                any(),
                any()
        );
    }
}
