package com.example.blog.notification.repository;

import com.example.blog.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByRecipientUserIdOrderByCreatedAtDescIdDesc(Long recipientUserId);

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    List<Notification> findAllByRecipientUserIdAndReadAtIsNull(Long recipientUserId);
}
