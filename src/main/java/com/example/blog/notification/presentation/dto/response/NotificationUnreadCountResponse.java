package com.example.blog.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "안 읽은 알림 수 응답")
public record NotificationUnreadCountResponse(
        @Schema(description = "안 읽은 알림 수", example = "3")
        long unreadCount
) {
}
