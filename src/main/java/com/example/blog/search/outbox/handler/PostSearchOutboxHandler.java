package com.example.blog.search.outbox.handler;

import com.example.blog.search.monitoring.SearchSyncMetrics;
import com.example.blog.search.outbox.dto.PostOutboxPayload;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.service.OutboxPayloadSerializer;
import com.example.blog.search.service.PostSearchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PostSearchOutboxHandler implements OutboxEventHandler {

    private final OutboxPayloadSerializer serializer;
    private final PostSearchSyncService postSearchSyncService;
    private final SearchSyncMetrics metrics;

    @Override
    public boolean supports(OutboxEvent event) {
        return "POST".equals(event.getAggregateType());
    }

    @Override
    public void handle(OutboxEvent event) {
        PostOutboxPayload payload = serializer.deserialize(event.getPayload(), PostOutboxPayload.class);

        if (event.getEventType() == OutboxEventType.DELETED) {
            postSearchSyncService.delete(payload.getPostId(), payload.getVersion());
            return;
        }

        postSearchSyncService.syncPostToSearch(payload.getPostId());
    }

    @Override
    public void onSuccess(OutboxEvent event, Duration duration) {
        String eventType = event.getEventType().name().toLowerCase();
        metrics.incrementSyncSuccess(eventType);
        metrics.recordProcessingLatency(eventType, duration);
    }

    @Override
    public void onFailure(OutboxEvent event, Exception exception) {
        metrics.incrementSyncFailure(event.getEventType().name().toLowerCase());
    }

    @Override
    public void onRetry(OutboxEvent event) {
        metrics.incrementRetry(event.getEventType().name().toLowerCase());
    }
}
