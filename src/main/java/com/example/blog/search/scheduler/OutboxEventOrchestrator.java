package com.example.blog.search.scheduler;

import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.outbox.handler.OutboxEventHandler;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventOrchestrator {

    private final OutboxEventRepository outboxEventRepository;
    private final List<OutboxEventHandler> handlers;
    private final OutboxEventStateService outboxEventStateService;

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

        OutboxEventHandler handler = resolveHandler(snapshot);

        boolean locked = outboxEventStateService.markProcessing(eventId);
        if (!locked) {
            log.debug("PROCESSING 전환 실패(이미 다른 상태이거나 선점됨). eventId={}", eventId);
            return;
        }

        try {
            OutboxEvent processingEvent = outboxEventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("Outbox event not found after markProcessing. id=" + eventId));

            handler.handle(processingEvent);

            outboxEventStateService.markSuccess(eventId);
            handler.onSuccess(processingEvent, Duration.between(processingEvent.getCreatedAt(), LocalDateTime.now()));

            log.info("Outbox 처리 성공. eventId={}, aggregateId={}, eventType={}",
                    processingEvent.getId(), processingEvent.getAggregateId(), processingEvent.getEventType());

        } catch (Exception e) {
            log.error("Outbox 처리 실패. eventId={}", eventId, e);

            handler.onFailure(snapshot, e);
            boolean requeued = outboxEventStateService.markFailureAndRequeue(eventId, e);
            if (requeued) {
                handler.onRetry(snapshot);
            }
        }
    }

    private OutboxEventHandler resolveHandler(OutboxEvent event) {
        return handlers.stream()
                .filter(handler -> handler.supports(event))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No outbox handler for aggregateType=%s, eventType=%s".formatted(
                                event.getAggregateType(),
                                event.getEventType()
                        )
                ));
    }
}
