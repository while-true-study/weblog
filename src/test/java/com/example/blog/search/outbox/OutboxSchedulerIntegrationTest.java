package com.example.blog.search.outbox;

import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.scheduler.OutboxEventOrchestrator;
import com.example.blog.search.service.PostSearchSyncService;
import com.example.blog.support.IntegrationTestSupport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

class OutboxSchedulerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OutboxEventOrchestrator outboxEventOrchestrator;

    @MockitoBean
    private PostSearchSyncService postSearchSyncService;

    @Test
    @DisplayName("PENDING CREATED 이벤트가 성공하면 SUCCESS로 전환된다")
    void pendingEvent_whenSyncSucceeds_marksSuccess() {
        OutboxEvent event = saveEvent(OutboxEventType.CREATED, 101L, 1L);

        doNothing().when(postSearchSyncService).syncPostToSearch(101L);

        outboxEventOrchestrator.process(event.getId());

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.SUCCESS);
        assertThat(updated.getProcessedAt()).isNotNull();
        assertThat(updated.getLastErrorMessage()).isNull();
        assertThat(updated.getRetryCount()).isZero();

        verify(postSearchSyncService).syncPostToSearch(101L);
    }

    @Test
    @DisplayName("PENDING CREATED 이벤트가 실패하면 retry metadata를 남기고 재시도 대기 상태로 돌아간다")
    void pendingEvent_whenSyncFails_requeuesWithRetryMetadata() {
        OutboxEvent event = saveEvent(OutboxEventType.CREATED, 102L, 1L);
        LocalDateTime before = LocalDateTime.now();

        doThrow(new RuntimeException("sync failed")).when(postSearchSyncService).syncPostToSearch(102L);

        outboxEventOrchestrator.process(event.getId());

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(updated.getLastErrorMessage()).isEqualTo("sync failed");
        assertThat(updated.getNextRetryAt()).isAfter(before);
        assertThat(updated.getProcessedAt()).isNull();

        verify(postSearchSyncService).syncPostToSearch(102L);
    }

    @Test
    @DisplayName("최대 재시도 직전 이벤트가 한 번 더 실패하면 FAILED 상태로 남는다")
    void pendingEvent_whenMaxRetryExceeded_staysFailed() {
        OutboxEvent event = saveEvent(OutboxEventType.UPDATED, 103L, 5L);
        prepareForFinalRetry(event);

        doThrow(new RuntimeException("final failure")).when(postSearchSyncService).syncPostToSearch(103L);

        outboxEventOrchestrator.process(event.getId());

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getRetryCount()).isEqualTo(5);
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(updated.getLastErrorMessage()).isEqualTo("final failure");
        assertThat(updated.getProcessedAt()).isNull();

        verify(postSearchSyncService).syncPostToSearch(103L);
    }

    @Test
    @DisplayName("PENDING DELETED 이벤트가 성공하면 delete 동기화를 호출하고 SUCCESS로 전환된다")
    void deletedEvent_whenSyncSucceeds_marksSuccessAndCallsDelete() {
        OutboxEvent event = saveEvent(OutboxEventType.DELETED, 104L, 7L);

        doNothing().when(postSearchSyncService).delete(104L, 7L);

        outboxEventOrchestrator.process(event.getId());

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.SUCCESS);
        assertThat(updated.getProcessedAt()).isNotNull();
        assertThat(updated.getRetryCount()).isZero();
        assertThat(updated.getLastErrorMessage()).isNull();

        verify(postSearchSyncService).delete(104L, 7L);
    }

    private OutboxEvent saveEvent(OutboxEventType eventType, Long postId, Long version) {
        String payload = objectMapper.createObjectNode()
                .put("postId", postId)
                .put("version", version)
                .toString();

        OutboxEvent event = new OutboxEvent("POST", postId, eventType, payload, version);
        return outboxEventRepository.save(event);
    }

    private void prepareForFinalRetry(OutboxEvent event) {
        for (int i = 0; i < 4; i++) {
            event.markFailed("previous failure " + i);
            event.requeueIfRetryable();
        }
        ReflectionTestUtils.setField(event, "nextRetryAt", LocalDateTime.now().minusSeconds(1));
        outboxEventRepository.save(event);
    }
}
