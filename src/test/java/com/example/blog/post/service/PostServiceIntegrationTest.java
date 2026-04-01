package com.example.blog.post.service;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.presentation.dto.request.PostPublishedDto;
import com.example.blog.post.presentation.dto.request.PostUpdateRequest;
import com.example.blog.post.presentation.dto.response.PostCreateResponse;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserRole;
import com.example.blog.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Testcontainers
class PostServiceIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0.36");

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private User author;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        postRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("test@example.com");
        user.setNickname("tester");
        user.setUsername("tester");
        user.setPassword("encoded-password");
        user.setRole(UserRole.USER); // 네 enum 이름에 맞게 수정

        author = userRepository.save(user);
    }

    @Test
    @DisplayName("게시글 생성 시 Post 저장과 CREATED outbox 이벤트 생성")
    void createPost_createsPostAndOutbox() {
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
        assertThat(savedPost.getPostStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(savedPost.getSyncVersion()).isNotNull();

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAggregateId()).isEqualTo(savedPost.getPostId());
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