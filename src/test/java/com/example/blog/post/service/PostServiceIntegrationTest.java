package com.example.blog.post.service;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.presentation.dto.request.PostPublishedDto;
import com.example.blog.post.presentation.dto.request.PostUpdateRequest;
import com.example.blog.post.presentation.dto.response.PostCreateResponse;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private PostService postService;

    private User author;

    @BeforeEach
    void setUpAuthor() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setNickname("tester");
        user.setUsername("tester");
        user.setPassword("encoded-password");
        user.setRole(UserRole.USER);

        author = userRepository.save(user);
    }

    @Test
    @DisplayName("status=PUBLISHED로 게시글 생성 시 PUBLISHED 상태로 저장되고 CREATED outbox 이벤트가 생성된다")
    void createPost_withPublishedStatus_savesPublishedPostAndCreatesOutbox() {
        PostPublishedDto request = new PostPublishedDto(
                "제목",
                "본문",
                null,
                List.of("java", "spring"),
                PostStatus.PUBLISHED.name()
        );

        PostCreateResponse response = postService.createPost(request, author.getEmail());

        Post savedPost = postRepository.findById(response.id()).orElseThrow();
        assertThat(savedPost.getTitle()).isEqualTo("제목");
        assertThat(savedPost.getContent()).isEqualTo("본문");
        assertThat(savedPost.getPostStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(savedPost.getSyncVersion()).isNotNull();

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAggregateId()).isEqualTo(savedPost.getPostId());
        assertThat(events.get(0).getEventType()).isEqualTo(OutboxEventType.CREATED);
    }

    @Test
    @DisplayName("status=DRAFT로 게시글 생성 시 DRAFT 상태로 저장되고 CREATED outbox 이벤트가 생성된다")
    void createPost_withDraftStatus_savesDraftPostAndCreatesOutbox() {
        PostPublishedDto request = new PostPublishedDto(
                "제목",
                "본문",
                null,
                List.of("java", "spring"),
                PostStatus.DRAFT.name()
        );

        PostCreateResponse response = postService.createPost(request, author.getEmail());

        Post savedPost = postRepository.findById(response.id()).orElseThrow();
        assertThat(savedPost.getTitle()).isEqualTo("제목");
        assertThat(savedPost.getContent()).isEqualTo("본문");
        assertThat(savedPost.getPostStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(savedPost.getSyncVersion()).isNotNull();

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAggregateId()).isEqualTo(savedPost.getPostId());
        assertThat(events.get(0).getEventType()).isEqualTo(OutboxEventType.CREATED);
    }

    @Test
    @DisplayName("status가 누락되면 게시글은 기본값 PUBLISHED로 저장되고 CREATED outbox 이벤트가 생성된다")
    void createPost_withoutStatus_defaultsToPublished() {
        PostPublishedDto request = new PostPublishedDto(
                "기본 공개 제목",
                "기본 공개 본문",
                null,
                List.of("default"),
                null
        );

        PostCreateResponse response = postService.createPost(request, author.getEmail());

        Post savedPost = postRepository.findById(response.id()).orElseThrow();
        assertThat(savedPost.getPostStatus()).isEqualTo(PostStatus.PUBLISHED);

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo(OutboxEventType.CREATED);
    }

    @Test
    @DisplayName("status가 공백이면 게시글은 기본값 PUBLISHED로 저장되고 CREATED outbox 이벤트가 생성된다")
    void createPost_withBlankStatus_defaultsToPublished() {
        PostPublishedDto request = new PostPublishedDto(
                "공백 상태 제목",
                "공백 상태 본문",
                null,
                List.of("blank"),
                "   "
        );

        PostCreateResponse response = postService.createPost(request, author.getEmail());

        Post savedPost = postRepository.findById(response.id()).orElseThrow();
        assertThat(savedPost.getPostStatus()).isEqualTo(PostStatus.PUBLISHED);

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo(OutboxEventType.CREATED);
    }

    @Test
    @DisplayName("게시글 수정 시 syncVersion 증가와 UPDATED outbox 이벤트 생성")
    void updatePost_updatesSyncVersionAndCreatesOutbox() {
        Post post = new Post();
        post.setTitle("기존 제목");
        post.setContent("기존 본문");
        post.setAuthor(author);
        post.setPostStatus(PostStatus.PUBLISHED);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        post = postRepository.save(post);

        Long beforeVersion = post.getSyncVersion();

        PostUpdateRequest request = new PostUpdateRequest();
        request.setTitle("수정 제목");
        request.setContent("수정 본문");

        postService.updatePost(post.getPostId(), request, author.getEmail());

        Post updatedPost = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(updatedPost.getTitle()).isEqualTo("수정 제목");
        assertThat(updatedPost.getContent()).isEqualTo("수정 본문");
        assertThat(updatedPost.getSyncVersion()).isGreaterThan(beforeVersion);

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAggregateId()).isEqualTo(updatedPost.getPostId());
        assertThat(events.get(0).getEventType()).isEqualTo(OutboxEventType.UPDATED);
    }

    @Test
    @DisplayName("게시글 삭제 시 soft delete와 DELETED outbox 이벤트 생성")
    void deletePost_softDeletesAndCreatesOutbox() {
        Post post = new Post();
        post.setTitle("삭제 대상");
        post.setContent("삭제 본문");
        post.setAuthor(author);
        post.setPostStatus(PostStatus.PUBLISHED);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        post = postRepository.save(post);

        postService.deletePost(post.getPostId(), author.getEmail());

        Post deletedPost = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(deletedPost.getPostStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(deletedPost.getDeletedAt()).isNotNull();

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAggregateId()).isEqualTo(deletedPost.getPostId());
        assertThat(events.get(0).getEventType()).isEqualTo(OutboxEventType.DELETED);
    }
}
