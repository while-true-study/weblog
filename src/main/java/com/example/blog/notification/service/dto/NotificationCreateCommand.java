package com.example.blog.notification.service.dto;

import com.example.blog.notification.entity.NotificationTargetType;
import com.example.blog.notification.entity.NotificationType;

public record NotificationCreateCommand(
        Long recipientUserId,
        NotificationType type,
        String title,
        String message,
        NotificationTargetType targetType,
        Long targetId
) {
}
