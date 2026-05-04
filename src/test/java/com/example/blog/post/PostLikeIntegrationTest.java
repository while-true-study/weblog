package com.example.blog.post;

import com.example.blog.popular.service.PopularEventService;
import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostLikeIntegrationTest extends IntegrationTestSupport {

    @MockitoBean
    private PopularEventService popularEventService;

    @Test
    @DisplayName("미인증 사용자는 좋아요 API 호출 시 401을 받는다")
    void unauthenticatedUser_cannotToggleLike() throws Exception {
        User author = createUser("like-auth-1@test.com", "like-auth-1", "like-auth-1");
        Post post = createPost(author, "unauthorized title", "unauthorized content");

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("A001"))
                .andExpect(jsonPath("$.error.message").value("인증이 필요합니다."));

        assertThat(postLikeRepository.findAll()).isEmpty();
        assertThat(postRepository.findById(post.getPostId()).orElseThrow().getLikeCount()).isZero();
    }

    @Test
    @DisplayName("인증 사용자는 좋아요를 추가할 수 있다")
    void authenticatedUser_canAddLike() throws Exception {
        User user = createUser("like-user-1@test.com", "like-user-1", "like-user-1");
        Post post = createPost(user, "liked title", "liked content");
        Long beforeVersion = post.getSyncVersion();

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(1L);
        assertThat(updated.getSyncVersion()).isGreaterThan(beforeVersion);
        assertThat(postLikeRepository.findByPostPostIdAndUserUserId(post.getPostId(), user.getUserId())).isPresent();
        assertThat(postLikeRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("같은 사용자가 다시 호출하면 좋아요가 취소된다")
    void sameUser_secondToggleCancelsLike() throws Exception {
        User user = createUser("like-user-2@test.com", "like-user-2", "like-user-2");
        Post post = createPost(user, "toggle title", "toggle content");

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(updated.getLikeCount()).isZero();
        assertThat(postLikeRepository.findByPostPostIdAndUserUserId(post.getPostId(), user.getUserId())).isEmpty();
        assertThat(postLikeRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("같은 사용자 좋아요 토글을 여러 번 호출해도 중복 row는 생성되지 않는다")
    void repeatedToggle_doesNotCreateDuplicateRows() throws Exception {
        User user = createUser("like-user-3@test.com", "like-user-3", "like-user-3");
        Post post = createPost(user, "duplicate title", "duplicate content");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                            .servletPath("/api/v1")
                            .header("Authorization", bearerToken(user)))
                    .andExpect(status().isOk());
        }

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(1L);
        assertThat(postLikeRepository.findByPostPostIdAndUserUserId(post.getPostId(), user.getUserId())).isPresent();
        assertThat(postLikeRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("좋아요 추가 시 search outbox UPDATED 이벤트가 생성된다")
    void likeAdd_createsUpdatedOutboxEvent() throws Exception {
        User user = createUser("like-user-4@test.com", "like-user-4", "like-user-4");
        Post post = createPost(user, "outbox add title", "outbox add content");
        Long beforeVersion = post.getSyncVersion();

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk());

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(updated.getSyncVersion()).isGreaterThan(beforeVersion);
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(event.getEventType()).isEqualTo(OutboxEventType.UPDATED);
        assertThat(event.getAggregateId()).isEqualTo(post.getPostId());
        assertThat(event.getVersion()).isEqualTo(updated.getSyncVersion());
        assertThat(payload.get("postId").asLong()).isEqualTo(post.getPostId());
        assertThat(payload.get("version").asLong()).isEqualTo(updated.getSyncVersion());
    }

    @Test
    @DisplayName("좋아요 취소 시 search outbox UPDATED 이벤트가 생성된다")
    void likeCancel_createsUpdatedOutboxEvent() throws Exception {
        User user = createUser("like-user-5@test.com", "like-user-5", "like-user-5");
        Post post = createPost(user, "outbox cancel title", "outbox cancel content");

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk());

        outboxEventRepository.deleteAll();
        Long versionBeforeCancel = postRepository.findById(post.getPostId()).orElseThrow().getSyncVersion();

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        List<OutboxEvent> events = outboxEventRepository.findAll();

        assertThat(updated.getLikeCount()).isZero();
        assertThat(updated.getSyncVersion()).isGreaterThan(versionBeforeCancel);
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(event.getEventType()).isEqualTo(OutboxEventType.UPDATED);
        assertThat(event.getAggregateId()).isEqualTo(post.getPostId());
        assertThat(event.getVersion()).isEqualTo(updated.getSyncVersion());
        assertThat(payload.get("postId").asLong()).isEqualTo(post.getPostId());
        assertThat(payload.get("version").asLong()).isEqualTo(updated.getSyncVersion());
    }

    @Test
    @DisplayName("타인의 게시글에도 좋아요를 누를 수 있다")
    void otherUser_canLikeForeignPost() throws Exception {
        User author = createUser("like-author-1@test.com", "like-author-1", "like-author-1");
        User other = createUser("like-other-1@test.com", "like-other-1", "like-other-1");
        Post post = createPost(author, "foreign like title", "foreign like content");

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        assertThat(postRepository.findById(post.getPostId()).orElseThrow().getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("본인 게시글 좋아요는 현재 정책상 허용된다")
    void ownPost_currentlyAllowsLike() throws Exception {
        User author = createUser("like-author-2@test.com", "like-author-2", "like-author-2");
        Post post = createPost(author, "own like title", "own like content");

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @DisplayName("삭제된 게시글에는 좋아요를 누를 수 없다")
    void deletedPost_cannotBeLiked() throws Exception {
        User user = createUser("like-user-6@test.com", "like-user-6", "like-user-6");
        Post post = createPost(user, "deleted like title", "deleted like content");
        post.softDelete();
        postRepository.save(post);

        mockMvc.perform(post("/api/v1/posts/" + post.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P001"))
                .andExpect(jsonPath("$.error.message").value("게시글을 찾을 수 없습니다."));

        assertThat(postLikeRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("DRAFT 게시글 좋아요는 현재 정책상 허용된다")
    void draftPost_currentlyAllowsLike() throws Exception {
        User author = createUser("like-author-3@test.com", "like-author-3", "like-author-3");
        Post post = new Post();
        post.setAuthor(author);
        post.setTitle("draft like title");
        post.setContent("draft like content");
        post.setPostStatus(PostStatus.DRAFT);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        Post draftPost = postRepository.save(post);

        MvcResult result = mockMvc.perform(post("/api/v1/posts/" + draftPost.getPostId() + "/like")
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andReturn();

        Post updated = postRepository.findById(draftPost.getPostId()).orElseThrow();
        assertThat(updated.getPostStatus()).isEqualTo(PostStatus.DRAFT);
        assertThat(updated.getLikeCount()).isEqualTo(1L);
        assertThat(result.getResponse().getContentAsString()).contains("\"liked\":true");
    }
}
