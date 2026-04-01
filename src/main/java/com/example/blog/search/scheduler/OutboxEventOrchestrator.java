package com.example.blog.search.scheduler;

import com.example.blog.search.monitoring.SearchSyncMetrics;
import com.example.blog.search.outbox.dto.PostOutboxPayload;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import com.example.blog.search.outbox.service.OutboxPayloadSerializer;
import com.example.blog.search.service.PostSearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventOrchestrator {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadSerializer serializer;
    private final PostSearchSyncService postSearchSyncService;
    private final OutboxEventStateService outboxEventStateService;
    private final SearchSyncMetrics metrics;

    public void process(Long eventId) {
        OutboxEvent snapshot = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found. id=" + eventId));

        if (snapshot.getStatus() != OutboxEventStatus.PENDING) {
            log.debug("이미 처리 대상이 아닙니다. eventId={}, status={}", snapshot.getId(), snapshot.getStatus());
            return;
        }

        if (snapshot.getNextRetryAt() != null && snapshot.getNextRetryAt().isAfter(LocalDateTime.now())) {
            log.debug("아직 재시도 시각이 되지 않았습니다. eventId={}, nextRetryAt={}",
                    snapshot.getId(), snapshot.getNextRetryAt());
            return;
        }

        String eventType = snapshot.getEventType().name().toLowerCase();

        boolean locked = outboxEventStateService.markProcessing(eventId);
        if (!locked) {
            log.debug("PROCESSING 전환 실패(이미 다른 상태이거나 선점됨). eventId={}", eventId);
            return;
        }

        try {
            OutboxEvent processingEvent = outboxEventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Outbox event not found after markProcessing. id=" + eventId));

            PostOutboxPayload payload = serializer.deserialize(processingEvent.getPayload());

            if (processingEvent.getEventType() == OutboxEventType.DELETED) {
                postSearchSyncService.delete(payload.getPostId(), payload.getVersion());
            } else {
                postSearchSyncService.syncPostToSearch(payload.getPostId());
            }

            outboxEventStateService.markSuccess(eventId);

            metrics.incrementSyncSuccess(eventType);
            metrics.recordProcessingLatency(
                    eventType,
                    Duration.between(processingEvent.getCreatedAt(), LocalDateTime.now())
            );

            log.info("Outbox 처리 성공. eventId={}, aggregateId={}, eventType={}",
                    processingEvent.getId(), processingEvent.getAggregateId(), processingEvent.getEventType());

        } catch (Exception e) {
            log.error("Outbox 처리 실패. eventId={}", eventId, e);

            metrics.incrementSyncFailure(eventType);
            outboxEventStateService.markFailureAndRequeue(eventId, eventType, e);
        }
    }
}