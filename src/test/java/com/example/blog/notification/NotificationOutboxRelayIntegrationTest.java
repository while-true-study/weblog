package com.example.blog.notification;

import com.example.blog.comment.presentation.dto.request.CommentCreateRequest;
import com.example.blog.comment.service.CommentService;
import com.example.blog.notification.outbox.dto.NotificationOutboxPayload;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.service.OutboxPayloadSerializer;
import com.example.blog.search.scheduler.OutboxEventOrchestrator;
import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationOutboxRelayIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CommentService commentService;

    @Autowired
    private OutboxEventOrchestrator outboxEventOrchestrator;

    @Autowired
    private OutboxPayloadSerializer outboxPayloadSerializer;

    @Test
    @DisplayName("댓글 생성 outbox 이벤트는 notification handler를 통해 알림으로 저장된다")
    void commentCreatedOutboxEvent_createsNotification() {
        User author = createUser("relay-author@test.com", "relay-author", "relay-author");
        User commenter = createUser("relay-commenter@test.com", "relay-commenter", "relay-commenter");

        var post = createPost(author, "notification relay", "content");
        var response = commentService.createComment(post.getPostId(), commenter.getUserId(), new CommentCreateRequest("첫 댓글"));

        OutboxEvent event = outboxEventRepository.findAll().get(0);
        assertThat(event.getAggregateType()).isEqualTo("COMMENT");
        assertThat(event.getAggregateId()).isEqualTo(response.id());
        assertThat(event.getEventType()).isEqualTo(OutboxEventType.CREATED);

        outboxEventOrchestrator.process(event.getId());

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.SUCCESS);
        assertThat(notificationRepository.findAll()).hasSize(1);
        assertThat(notificationRepository.findAll().get(0).getRecipientUserId()).isEqualTo(author.getUserId());
        assertThat(notificationRepository.findAll().get(0).getTargetId()).isEqualTo(response.id());
    }

    @Test
    @DisplayName("작성자가 자기 글에 댓글을 달면 notification outbox 이벤트를 만들지 않는다")
    void selfComment_doesNotCreateNotificationOutboxEvent() {
        User author = createUser("relay-self@test.com", "relay-self", "relay-self");
        var post = createPost(author, "self comment post", "content");

        commentService.createComment(post.getPostId(), author.getUserId(), new CommentCreateRequest("내가 쓴 댓글"));

        assertThat(outboxEventRepository.findAll()).isEmpty();
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("notification handler가 실패하면 retry metadata를 남기고 재시도 대기 상태로 돌아간다")
    void notificationHandlerFailure_requeuesWithRetryMetadata() {
        NotificationOutboxPayload payload = new NotificationOutboxPayload(
                999999L,
                123L,
                com.example.blog.notification.entity.NotificationType.ACTIVITY,
                "새 댓글이 달렸습니다.",
                "알림 처리 실패 테스트",
                com.example.blog.notification.entity.NotificationTargetType.ACTIVITY,
                77L
        );

        OutboxEvent event = outboxEventRepository.save(
                new OutboxEvent("COMMENT", 77L, OutboxEventType.CREATED, outboxPayloadSerializer.serialize(payload), 1L)
        );

        outboxEventOrchestrator.process(event.getId());

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getLastErrorMessage()).contains("사용자를 찾을 수 없습니다.");
        assertThat(updated.getProcessedAt()).isNull();
    }
}
