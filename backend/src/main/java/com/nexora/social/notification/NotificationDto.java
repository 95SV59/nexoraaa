package com.nexora.social.notification;

import java.time.LocalDateTime;

public record NotificationDto(
    Long id,
    NotificationType type,
    String title,
    String message,
    String link,
    boolean read,
    LocalDateTime createdAt
) {}
