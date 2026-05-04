package com.example.blog.notification.presentation.dto.response;

import com.example.blog.notification.entity.Notification;
import com.example.blog.notification.entity.NotificationTargetType;
import com.example.blog.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 항목 응답")
public record NotificationItemResponse(
        @Schema(description = "알림 ID", example = "1")
        Long id,
        @Schema(description = "알림 타입", example = "SYSTEM")
        NotificationType type,
        @Schema(description = "알림 제목", example = "새 알림이 도착했습니다.")
        String title,
        @Schema(description = "알림 메시지", example = "프로젝트에 새로운 활동이 등록되었습니다.")
        String message,
        @Schema(description = "알림 대상 타입", example = "ACTIVITY", nullable = true)
        NotificationTargetType targetType,
        @Schema(description = "알림 대상 ID", example = "42", nullable = true)
        Long targetId,
        @Schema(description = "읽음 여부", example = "false")
        boolean read,
        @Schema(description = "읽은 시각", nullable = true)
        LocalDateTime readAt,
        @Schema(description = "생성 시각")
        LocalDateTime createdAt
) {
    public static NotificationItemResponse from(Notification notification) {
        return new NotificationItemResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
