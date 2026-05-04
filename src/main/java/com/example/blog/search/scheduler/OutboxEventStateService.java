package com.example.blog.search.scheduler;

import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventStateService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessing(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found. id=" + eventId));

        if (event.getStatus() != OutboxEventStatus.PENDING) {
            return false;
        }

        event.markProcessing();
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found. id=" + eventId));

        event.markSuccess();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markFailureAndRequeue(Long eventId, Exception e) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found. id=" + eventId));

        if (event.getStatus() != OutboxEventStatus.PROCESSING
                && event.getStatus() != OutboxEventStatus.PENDING) {
            log.warn("실패 처리 대상 상태가 예상과 다릅니다. eventId={}, status={}", eventId, event.getStatus());
        }

        event.markFailed(truncateMessage(e.getMessage()));

        if (event.canRetry()) {
            event.requeueIfRetryable();
            log.warn("Outbox 재시도 예정. eventId={}, retryCount={}, nextRetryAt={}",
                    event.getId(), event.getRetryCount(), event.getNextRetryAt());
            return true;
        } else {
            log.error("Outbox 최종 실패. eventId={}, retryCount={}",
                    event.getId(), event.getRetryCount());
            return false;
        }
    }

    private String truncateMessage(String message) {
        if (message == null || message.isBlank()) {
            return "unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
