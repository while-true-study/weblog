package com.example.blog.search.scheduler;

import com.example.blog.search.outbox.dto.PostOutboxPayload;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import com.example.blog.search.outbox.service.OutboxPayloadSerializer;
import com.example.blog.search.service.PostSearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadSerializer serializer;
    private final PostSearchSyncService postSearchSyncService;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findByStatusAndNextRetryAtLessThanEqualOrderByIdAsc(
                        OutboxEventStatus.PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, 100)
                );

        for (OutboxEvent event : events) {
            try {
                event.markProcessing();

                PostOutboxPayload payload = serializer.deserialize(event.getPayload());

                if (event.getEventType() == OutboxEventType.DELETED) {
                    postSearchSyncService.delete(payload.getPostId(), payload.getVersion());
                } else {
                    postSearchSyncService.upsert(payload);
                }

                event.markSuccess();
            } catch (Exception e) {
                log.error("Outbox 처리 실패. eventId={}, aggregateId={}", event.getId(), event.getAggregateId(), e);

                event.markFailed(e.getMessage());

                if (event.canRetry()) {
                    event.requeueIfRetryable();
                }
            }
        }
    }
}