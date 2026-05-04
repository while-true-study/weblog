package com.example.blog.security;

import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.presentation.dto.request.SignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiAccessControlIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("미인증 사용자는 GET /api/v1/auth/me 접근 시 401을 받는다")
    void unauthenticatedUser_cannotAccessMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .servletPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미인증 사용자는 POST /api/v1/posts 접근 시 401을 받는다")
    void unauthenticatedUser_cannotCreatePost() throws Exception {
        String requestBody = """
                {"title":"제목","content":"본문","tags":["java"],"status":"PUBLISHED"}
                """;

        mockMvc.perform(post("/api/v1/posts")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미인증 사용자는 PATCH /api/v1/posts/{id} 접근 시 401을 받는다")
    void unauthenticatedUser_cannotPatchPost() throws Exception {
        String requestBody = """
                {"title":"수정 제목"}
                """;

        mockMvc.perform(patch("/api/v1/posts/999")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미인증 사용자는 DELETE /api/v1/posts/{id} 접근 시 401을 받는다")
    void unauthenticatedUser_cannotDeletePost() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/999")
                        .servletPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미인증 사용자는 GET /api/v1/series 접근 시 401을 받는다")
    void unauthenticatedUser_cannotAccessSeries() throws Exception {
        mockMvc.perform(get("/api/v1/series")
                        .servletPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("공개 게시글 목록 API는 미인증 사용자도 접근 가능하다")
    void publicPostsList_isAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/posts")
                        .servletPath("/api/v1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 검색 API는 미인증 사용자도 접근 가능하다")
    void publicSearchApi_isAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/search/posts")
                        .servletPath("/api/v1")
                        .param("keyword", "spring"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원가입 API는 미인증 사용자도 접근 가능하다")
    void signupApi_isAccessibleWithoutAuthentication() throws Exception {
        SignupRequest request = new SignupRequest(
                "public-signup@example.com",
                "password123",
                "public-user",
                "public-user"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
