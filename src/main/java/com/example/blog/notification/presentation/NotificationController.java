package com.example.blog.notification.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.global.exception.BlogException;
import com.example.blog.global.exception.ErrorCode;
import com.example.blog.notification.presentation.dto.response.NotificationItemResponse;
import com.example.blog.notification.presentation.dto.response.NotificationReadAllResponse;
import com.example.blog.notification.presentation.dto.response.NotificationUnreadCountResponse;
import com.example.blog.notification.service.NotificationService;
import com.example.blog.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notification", description = "사용자별 인앱 알림 조회/읽음 처리 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 조회", description = "로그인한 사용자의 알림 목록을 최신순으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<NotificationItemResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success(notificationService.getMyNotifications(requireUserId(principal)));
    }

    @Operation(summary = "내 안 읽은 알림 수 조회", description = "로그인한 사용자의 안 읽은 알림 개수를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/unread-count")
    public ApiResponse<NotificationUnreadCountResponse> getMyUnreadCount(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success(notificationService.getMyUnreadCount(requireUserId(principal)));
    }

    @Operation(summary = "알림 읽음 처리", description = "로그인한 사용자의 특정 알림을 읽음 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationItemResponse> markAsRead(
            @Parameter(description = "알림 ID") @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success(
                notificationService.markAsRead(notificationId, requireUserId(principal))
        );
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "로그인한 사용자의 읽지 않은 알림을 모두 읽음 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/read-all")
    public ApiResponse<NotificationReadAllResponse> markAllAsRead(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success(notificationService.markAllAsRead(requireUserId(principal)));
    }

    private Long requireUserId(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new BlogException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getId();
    }
}
