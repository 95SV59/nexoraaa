package com.nexora.social.presence;

import com.nexora.user.User;
import com.nexora.user.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(PresenceRepository presenceRepository,
                           UserRepository userRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.presenceRepository = presenceRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public PresenceDto updatePresence(Long userId, PresenceStatus status, String gameSlug, String roomCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserPresence presence = presenceRepository.findByUserId(userId)
                .orElseGet(() -> new UserPresence(user, status));

        presence.setStatus(status);
        presence.setCurrentGameSlug(gameSlug);
        presence.setCurrentRoomCode(roomCode);
        presence.setLastSeen(LocalDateTime.now());
        presenceRepository.save(presence);

        PresenceDto dto = toDto(presence);
        // Broadcast presence update over WebSocket
        messagingTemplate.convertAndSend("/topic/presence", dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public PresenceDto getPresence(Long userId) {
        return presenceRepository.findByUserId(userId)
                .map(this::toDto)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElse(null);
                    return new PresenceDto(
                            userId,
                            user != null ? user.getUsername() : "Unknown",
                            user != null ? user.getAvatarUrl() : null,
                            PresenceStatus.OFFLINE,
                            null, null,
                            LocalDateTime.now()
                    );
                });
    }

    @Transactional(readOnly = true)
    public List<PresenceDto> getPresences(List<Long> userIds) {
        return presenceRepository.findByUserIdIn(userIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PresenceDto toDto(UserPresence presence) {
        return new PresenceDto(
                presence.getUserId(),
                presence.getUser().getUsername(),
                presence.getUser().getAvatarUrl(),
                presence.getStatus(),
                presence.getCurrentGameSlug(),
                presence.getCurrentRoomCode(),
                presence.getLastSeen()
        );
    }
}
