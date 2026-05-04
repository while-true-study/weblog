package com.example.blog.notification.service;

import com.example.blog.notification.presentation.dto.response.NotificationItemResponse;
import com.example.blog.notification.presentation.dto.response.NotificationReadAllResponse;
import com.example.blog.notification.presentation.dto.response.NotificationUnreadCountResponse;
import com.example.blog.notification.service.dto.NotificationCreateCommand;

import java.util.List;

public interface NotificationService {

    NotificationItemResponse createNotification(NotificationCreateCommand command);

    List<NotificationItemResponse> getMyNotifications(Long recipientUserId);

    NotificationUnreadCountResponse getMyUnreadCount(Long recipientUserId);

    NotificationItemResponse markAsRead(Long notificationId, Long recipientUserId);

    NotificationReadAllResponse markAllAsRead(Long recipientUserId);
}
