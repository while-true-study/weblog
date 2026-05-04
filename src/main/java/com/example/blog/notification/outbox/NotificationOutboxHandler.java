package com.example.blog.notification.outbox;

import com.example.blog.notification.outbox.dto.NotificationOutboxPayload;
import com.example.blog.notification.service.NotificationService;
import com.example.blog.notification.service.dto.NotificationCreateCommand;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.handler.OutboxEventHandler;
import com.example.blog.search.outbox.service.OutboxPayloadSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationOutboxHandler implements OutboxEventHandler {

    private final OutboxPayloadSerializer serializer;
    private final NotificationService notificationService;
    private final NotificationHandlerDelayInjector delayInjector;

    @Override
    public boolean supports(OutboxEvent event) {
        return "COMMENT".equals(event.getAggregateType()) && event.getEventType() == OutboxEventType.CREATED;
    }

    @Override
    public void handle(OutboxEvent event) {
        delayInjector.delayBeforeHandle();

        NotificationOutboxPayload payload = serializer.deserialize(event.getPayload(), NotificationOutboxPayload.class);

        notificationService.createNotification(new NotificationCreateCommand(
                payload.recipientUserId(),
                payload.notificationType(),
                payload.title(),
                payload.message(),
                payload.targetType(),
                payload.targetId()
        ));
    }
}
