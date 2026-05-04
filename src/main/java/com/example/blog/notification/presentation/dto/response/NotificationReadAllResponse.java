package com.example.blog.notification.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전체 읽음 처리 응답")
public record NotificationReadAllResponse(
        @Schema(description = "읽음 처리된 알림 수", example = "5")
        int updatedCount
) {
}
