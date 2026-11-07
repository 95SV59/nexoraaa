package com.nexora.social.notification;

import com.nexora.user.Role;
import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Platform Notification Service Unit Tests")
class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private SimpMessagingTemplate messagingTemplate;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);

        notificationService = new NotificationService(notificationRepository, userRepository, messagingTemplate);
    }

    @Test
    @DisplayName("Sends real-time notification via STOMP and persists entity")
    void testSendNotificationDispatchesWebSocketMessage() {
        Long targetUserId = 100L;
        User user = new User("tester", "test@nexora.gg", "hash", new Role("USER"));
        user.setId(targetUserId);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(user));

        Notification mockNotification = new Notification(user, NotificationType.FRIEND_REQUEST, "Friend Request", "Alice added you", "/profile");
        mockNotification.setId(1L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        NotificationDto dto = notificationService.sendNotification(
                targetUserId, NotificationType.FRIEND_REQUEST, "Friend Request", "Alice added you", "/profile"
        );

        assertNotNull(dto);
        assertEquals("Friend Request", dto.title());
        assertEquals("Alice added you", dto.message());

        // Verify WebSocket dispatch to specific user queue
        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/user/100/notifications"), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Calculates unread notification count correctly")
    void testGetUnreadCount() {
        Long userId = 101L;
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(4L);

        long count = notificationService.getUnreadCount(userId);
        assertEquals(4L, count);
        verify(notificationRepository, times(1)).countByUserIdAndReadFalse(userId);
    }

    @Test
    @DisplayName("Marks single notification as read if user matches")
    void testMarkAsRead() {
        Long userId = 102L;
        User user = new User("tester2", "test2@nexora.gg", "hash", new Role("USER"));
        user.setId(userId);

        Notification notification = new Notification(user, NotificationType.SYSTEM, "Update", "System maintenance", null);
        notification.setId(50L);
        notification.setRead(false);

        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(50L, userId);

        assertTrue(notification.isRead());
        verify(notificationRepository, times(1)).save(notification);
    }
}
