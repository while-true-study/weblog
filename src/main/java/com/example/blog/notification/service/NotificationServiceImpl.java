package com.example.blog.notification.service;

import com.example.blog.global.exception.BlogException;
import com.example.blog.global.exception.ErrorCode;
import com.example.blog.notification.entity.Notification;
import com.example.blog.notification.presentation.dto.response.NotificationItemResponse;
import com.example.blog.notification.presentation.dto.response.NotificationReadAllResponse;
import com.example.blog.notification.presentation.dto.response.NotificationUnreadCountResponse;
import com.example.blog.notification.repository.NotificationRepository;
import com.example.blog.notification.service.dto.NotificationCreateCommand;
import com.example.blog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public NotificationItemResponse createNotification(NotificationCreateCommand command) {
        if (!userRepository.existsById(command.recipientUserId())) {
            throw new BlogException(ErrorCode.USER_NOT_FOUND);
        }

        Notification notification = Notification.create(
                command.recipientUserId(),
                command.type(),
                command.title(),
                command.message(),
                command.targetType(),
                command.targetId()
        );

        return NotificationItemResponse.from(notificationRepository.save(notification));
    }

    @Override
    public List<NotificationItemResponse> getMyNotifications(Long recipientUserId) {
        return notificationRepository.findAllByRecipientUserIdOrderByCreatedAtDescIdDesc(recipientUserId).stream()
                .map(NotificationItemResponse::from)
                .toList();
    }

    @Override
    public NotificationUnreadCountResponse getMyUnreadCount(Long recipientUserId) {
        return new NotificationUnreadCountResponse(
                notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipientUserId)
        );
    }

    @Override
    @Transactional
    public NotificationItemResponse markAsRead(Long notificationId, Long recipientUserId) {
        Notification notification = notificationRepository.findByIdAndRecipientUserId(notificationId, recipientUserId)
                .orElseThrow(() -> new BlogException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markRead();
        return NotificationItemResponse.from(notification);
    }

    @Override
    @Transactional
    public NotificationReadAllResponse markAllAsRead(Long recipientUserId) {
        List<Notification> unreadNotifications = notificationRepository.findAllByRecipientUserIdAndReadAtIsNull(recipientUserId);

        int updatedCount = 0;
        for (Notification unreadNotification : unreadNotifications) {
            if (unreadNotification.markRead()) {
                updatedCount += 1;
            }
        }

        return new NotificationReadAllResponse(updatedCount);
    }
}
