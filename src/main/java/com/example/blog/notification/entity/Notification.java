package com.example.blog.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notification_recipient_created", columnList = "recipient_user_id, created_at"),
                @Index(name = "idx_notification_recipient_read_at", columnList = "recipient_user_id, read_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NotificationTargetType targetType;

    private Long targetId;

    private LocalDateTime readAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private Notification(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            NotificationTargetType targetType,
            Long targetId,
            LocalDateTime readAt,
            LocalDateTime createdAt
    ) {
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetType = targetType;
        this.targetId = targetId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public static Notification create(
            Long recipientUserId,
            NotificationType type,
            String title,
            String message,
            NotificationTargetType targetType,
            Long targetId
    ) {
        return Notification.builder()
                .recipientUserId(recipientUserId)
                .type(type)
                .title(title)
                .message(message)
                .targetType(targetType)
                .targetId(targetId)
                .build();
    }

    public boolean isRead() {
        return this.readAt != null;
    }

    public boolean markRead() {
        if (this.readAt != null) {
            return false;
        }
        this.readAt = LocalDateTime.now();
        return true;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
