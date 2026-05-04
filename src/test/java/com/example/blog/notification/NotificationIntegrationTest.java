package com.example.blog.notification;

import com.example.blog.notification.entity.Notification;
import com.example.blog.notification.entity.NotificationTargetType;
import com.example.blog.notification.entity.NotificationType;
import com.example.blog.notification.presentation.dto.response.NotificationItemResponse;
import com.example.blog.notification.service.NotificationService;
import com.example.blog.notification.service.dto.NotificationCreateCommand;
import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private NotificationService notificationService;

    @Test
    @DisplayName("알림 생성 시 recipientUserId 기준의 안 읽은 알림이 저장된다")
    void createNotification_savesUnreadNotification() {
        User recipient = createUser("notification-user-1@test.com", "notify1", "notify1");

        NotificationItemResponse created = createNotification(
                recipient.getUserId(),
                NotificationType.SYSTEM,
                "시스템 알림",
                "새로운 알림이 도착했습니다.",
                null,
                null
        );

        Notification saved = notificationRepository.findById(created.id()).orElseThrow();
        assertThat(saved.getRecipientUserId()).isEqualTo(recipient.getUserId());
        assertThat(saved.getType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(saved.getTitle()).isEqualTo("시스템 알림");
        assertThat(saved.getMessage()).isEqualTo("새로운 알림이 도착했습니다.");
        assertThat(saved.getReadAt()).isNull();
    }

    @Test
    @DisplayName("사용자는 자신의 알림 목록만 최신순으로 조회할 수 있다")
    void user_canGetOnlyOwnNotifications() throws Exception {
        User owner = createUser("notification-user-2@test.com", "notify2", "notify2");
        User other = createUser("notification-user-3@test.com", "notify3", "notify3");

        createNotification(owner.getUserId(), NotificationType.SYSTEM, "첫 알림", "owner first", null, null);
        NotificationItemResponse latest = createNotification(
                owner.getUserId(),
                NotificationType.ACTIVITY,
                "둘째 알림",
                "owner second",
                NotificationTargetType.ACTIVITY,
                10L
        );
        createNotification(other.getUserId(), NotificationType.SYSTEM, "타인 알림", "other", null, null);

        mockMvc.perform(get("/api/v1/notifications")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(latest.id()))
                .andExpect(jsonPath("$.data[0].title").value("둘째 알림"))
                .andExpect(jsonPath("$.data[0].type").value("ACTIVITY"))
                .andExpect(jsonPath("$.data[0].read").value(false))
                .andExpect(jsonPath("$.data[1].title").value("첫 알림"));
    }

    @Test
    @DisplayName("사용자는 자신의 안 읽은 알림 수를 조회할 수 있다")
    void user_canGetUnreadCount() throws Exception {
        User owner = createUser("notification-user-4@test.com", "notify4", "notify4");

        NotificationItemResponse unread = createNotification(owner.getUserId(), NotificationType.SYSTEM, "안읽음", "unread", null, null);
        NotificationItemResponse read = createNotification(owner.getUserId(), NotificationType.SYSTEM, "읽음", "read", null, null);
        notificationService.markAsRead(read.id(), owner.getUserId());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        assertThat(notificationRepository.findById(unread.id()).orElseThrow().getReadAt()).isNull();
    }

    @Test
    @DisplayName("사용자는 자신의 알림을 읽음 처리할 수 있다")
    void user_canMarkOwnNotificationAsRead() throws Exception {
        User owner = createUser("notification-user-5@test.com", "notify5", "notify5");
        NotificationItemResponse notification = createNotification(
                owner.getUserId(),
                NotificationType.PORTFOLIO,
                "포트폴리오 알림",
                "포트폴리오가 업데이트되었습니다.",
                NotificationTargetType.PORTFOLIO,
                7L
        );

        mockMvc.perform(patch("/api/v1/notifications/" + notification.id() + "/read")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(notification.id()))
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty())
                .andExpect(jsonPath("$.data.targetType").value("PORTFOLIO"))
                .andExpect(jsonPath("$.data.targetId").value(7));

        assertThat(notificationRepository.findById(notification.id()).orElseThrow().getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("사용자는 자신의 알림을 전체 읽음 처리할 수 있다")
    void user_canMarkAllOwnNotificationsAsRead() throws Exception {
        User owner = createUser("notification-user-6@test.com", "notify6", "notify6");

        createNotification(owner.getUserId(), NotificationType.SYSTEM, "알림1", "message1", null, null);
        createNotification(owner.getUserId(), NotificationType.EPISODE, "알림2", "message2", NotificationTargetType.EPISODE, 22L);

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updatedCount").value(2));

        assertThat(notificationRepository.countByRecipientUserIdAndReadAtIsNull(owner.getUserId())).isZero();
    }

    @Test
    @DisplayName("다른 사용자의 알림은 읽음 처리할 수 없다")
    void otherUser_cannotMarkForeignNotificationAsRead() throws Exception {
        User owner = createUser("notification-user-7@test.com", "notify7", "notify7");
        User other = createUser("notification-user-8@test.com", "notify8", "notify8");
        NotificationItemResponse notification = createNotification(owner.getUserId(), NotificationType.SYSTEM, "비공개 알림", "private", null, null);

        mockMvc.perform(patch("/api/v1/notifications/" + notification.id() + "/read")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(other)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("N001"))
                .andExpect(jsonPath("$.error.message").value("알림을 찾을 수 없습니다."));

        assertThat(notificationRepository.findById(notification.id()).orElseThrow().getReadAt()).isNull();
    }

    @Test
    @DisplayName("미인증 사용자는 알림 목록 조회 시 401을 받는다")
    void unauthenticatedUser_cannotGetNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .servletPath("/api/v1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A001"));
    }

    private NotificationItemResponse createNotification(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            NotificationTargetType targetType,
            Long targetId
    ) {
        return notificationService.createNotification(
                new NotificationCreateCommand(
                        recipientUserId,
                        type,
                        title,
                        message,
                        targetType,
                        targetId
                )
        );
    }
}
