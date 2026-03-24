package com.example.blog.search.scheduler;

import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupSuccessEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);

        List<OutboxEvent> oldSuccessEvents =
                outboxEventRepository.findByStatusAndProcessedAtBefore(
                        OutboxEventStatus.SUCCESS,
                        threshold
                );

        if (!oldSuccessEvents.isEmpty()) {
            outboxEventRepository.deleteAllInBatch(oldSuccessEvents);
            log.info("Outbox cleanup 완료. 삭제 건수={}", oldSuccessEvents.size());
        }
    }
}
