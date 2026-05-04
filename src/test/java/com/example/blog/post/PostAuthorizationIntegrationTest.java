package com.example.blog.post;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostAuthorizationIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("작성자는 자신의 게시글을 수정할 수 있다")
    void author_canPatchOwnPost() throws Exception {
        User author = createUser("author-patch@example.com", "author-patch", "author-patch");
        Post post = createPost(author, "원본 제목", "원본 본문");
        Long beforeVersion = post.getSyncVersion();

        String requestBody = """
                {"title":"수정된 제목","content":"수정된 본문"}
                """;

        mockMvc.perform(patch("/api/v1/posts/" + post.getPostId())
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.postId").value(post.getPostId()))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                .andExpect(jsonPath("$.data.version").isNumber())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getContent()).isEqualTo("수정된 본문");
        assertThat(updated.getSyncVersion()).isGreaterThan(beforeVersion);
    }

    @Test
    @DisplayName("타인은 게시글을 수정할 수 없고 403을 받는다")
    void otherUser_cannotPatchForeignPost() throws Exception {
        User author = createUser("author-foreign-patch@example.com", "author-foreign-patch", "author-foreign-patch");
        User other = createUser("other-foreign-patch@example.com", "other-foreign-patch", "other-foreign-patch");
        Post post = createPost(author, "원본 제목", "원본 본문");

        String requestBody = """
                {"title":"침입 수정 제목","content":"침입 수정 본문"}
                """;

        mockMvc.perform(patch("/api/v1/posts/" + post.getPostId())
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P003"))
                .andExpect(jsonPath("$.error.message").value("게시글 수정/삭제 권한이 없습니다."));

        Post unchanged = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(unchanged.getTitle()).isEqualTo("원본 제목");
        assertThat(unchanged.getContent()).isEqualTo("원본 본문");
        assertThat(unchanged.getPostStatus()).isEqualTo(PostStatus.PUBLISHED);
    }

    @Test
    @DisplayName("작성자는 자신의 게시글을 삭제할 수 있다")
    void author_canDeleteOwnPost() throws Exception {
        User author = createUser("author-delete@example.com", "author-delete", "author-delete");
        Post post = createPost(author, "삭제 대상 제목", "삭제 대상 본문");

        mockMvc.perform(delete("/api/v1/posts/" + post.getPostId())
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(author)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Post deleted = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(deleted.getPostStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("타인은 게시글을 삭제할 수 없고 403을 받는다")
    void otherUser_cannotDeleteForeignPost() throws Exception {
        User author = createUser("author-foreign-delete@example.com", "author-foreign-delete", "author-foreign-delete");
        User other = createUser("other-foreign-delete@example.com", "other-foreign-delete", "other-foreign-delete");
        Post post = createPost(author, "삭제 방어 제목", "삭제 방어 본문");

        mockMvc.perform(delete("/api/v1/posts/" + post.getPostId())
                        .servletPath("/api/v1")
                        .header("Authorization", bearerToken(other)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("P003"))
                .andExpect(jsonPath("$.error.message").value("게시글 수정/삭제 권한이 없습니다."));

        Post stillExists = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(stillExists.getPostStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(stillExists.getDeletedAt()).isNull();
    }
}
