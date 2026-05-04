package com.example.blog.notification;

import com.example.blog.comment.presentation.dto.request.CommentCreateRequest;
import com.example.blog.comment.repository.CommentRepository;
import com.example.blog.comment.service.CommentService;
import com.example.blog.notification.service.NotificationService;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import com.example.blog.search.scheduler.OutboxEventOrchestrator;
import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

class NotificationOutboxFailureIsolationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private OutboxEventOrchestrator outboxEventOrchestrator;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("알림 처리 실패가 발생해도 원본 댓글 생성은 롤백되지 않는다")
    void notificationFailure_doesNotRollbackCommentCreation() {
        User author = createUser("failure-author@test.com", "failure-author", "failure-author");
        User commenter = createUser("failure-commenter@test.com", "failure-commenter", "failure-commenter");
        var post = createPost(author, "failure isolation post", "content");

        var response = commentService.createComment(post.getPostId(), commenter.getUserId(), new CommentCreateRequest("실패 분리 테스트"));
        OutboxEvent event = outboxEventRepository.findAll().get(0);

        doThrow(new RuntimeException("notification down"))
                .when(notificationService)
                .createNotification(any());

        outboxEventOrchestrator.process(event.getId());

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(commentRepository.findById(response.id())).isPresent();
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getLastErrorMessage()).isEqualTo("notification down");
        assertThat(notificationRepository.findAll()).isEmpty();
    }
}
