package com.example.blog.search.scheduler;

import com.example.blog.search.monitoring.SearchSyncMetrics;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventOrchestrator outboxEventOrchestrator;
    private final SearchSyncMetrics metrics;

    @Scheduled(fixedDelay = 3000)
    public void processOutboxEvents() {
        LocalDateTime now = LocalDateTime.now();

        updateBacklogMetrics();

        List<OutboxEvent> events = outboxEventRepository
                .findByStatusAndNextRetryAtLessThanEqualOrderByIdAsc(
                        OutboxEventStatus.PENDING,
                        now,
                        PageRequest.of(0, 100)
                );

        if (events.isEmpty()) {
            log.debug("처리할 Outbox 이벤트가 없습니다.");
            return;
        }

        log.info("Outbox 배치 처리 시작. size={}", events.size());

        for (OutboxEvent event : events) {
            try {
                outboxEventOrchestrator.process(event.getId());
            } catch (Exception e) {
                log.error("Outbox 오케스트레이션 실패. eventId={}", event.getId(), e);
            }
        }

        updateBacklogMetrics();
    }

    private void updateBacklogMetrics() {
        metrics.setPendingCount(outboxEventRepository.countByStatus(OutboxEventStatus.PENDING));
        metrics.setFailedCount(outboxEventRepository.countByStatus(OutboxEventStatus.FAILED));
    }
}