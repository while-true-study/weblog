package com.example.blog.notification.outbox.dto;

import com.example.blog.notification.entity.NotificationTargetType;
import com.example.blog.notification.entity.NotificationType;

public record NotificationOutboxPayload(
        Long recipientUserId,
        Long actorUserId,
        NotificationType notificationType,
        String title,
        String message,
        NotificationTargetType targetType,
        Long targetId
) {
}
