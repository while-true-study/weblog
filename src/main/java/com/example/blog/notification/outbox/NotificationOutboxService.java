package com.example.blog.notification.outbox;

import com.example.blog.notification.outbox.dto.NotificationOutboxPayload;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import com.example.blog.search.outbox.service.OutboxPayloadSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private static final long INITIAL_VERSION = 1L;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadSerializer serializer;

    public void createCommentCreatedEvent(Long commentId, NotificationOutboxPayload payload) {
        OutboxEvent event = new OutboxEvent(
                "COMMENT",
                commentId,
                OutboxEventType.CREATED,
                serializer.serialize(payload),
                INITIAL_VERSION
        );

        outboxEventRepository.save(event);
    }
}
