package com.example.blog.auth;

import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.presentation.dto.request.LoginRequest;
import com.example.blog.user.presentation.dto.request.SignupRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("회원가입은 성공 시 200을 반환하고 사용자를 생성한다")
    void signup_succeeds() throws Exception {
        SignupRequest request = new SignupRequest(
                "signup-success@example.com",
                "password123",
                "signup-user",
                "signup-user"
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userRepository.findByEmail("signup-success@example.com")).isPresent();
    }

    @Test
    @DisplayName("로그인 성공 시 accessToken, refreshToken, user를 반환한다")
    void login_returnsTokensAndUser() throws Exception {
        signup("login-success@example.com");

        LoginRequest request = new LoginRequest("login-success@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("login-success@example.com"))
                .andExpect(jsonPath("$.data.user.nickname").value("login-success"))
                .andExpect(jsonPath("$.data.user.username").value("login-success"))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    @DisplayName("refreshToken으로 accessToken을 재발급할 수 있다")
    void refresh_reissuesAccessToken() throws Exception {
        signup("refresh-success@example.com");

        String refreshToken = loginAndExtractRefreshToken("refresh-success@example.com");

        String requestBody = """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").value(refreshToken));
    }

    @Test
    @DisplayName("잘못된 refreshToken 타입으로 요청하면 401을 반환한다")
    void refresh_withAccessToken_returnsUnauthorized() throws Exception {
        signup("invalid-refresh@example.com");

        String accessToken = loginAndExtractAccessToken("invalid-refresh@example.com");

        String requestBody = """
                {"refreshToken":"%s"}
                """.formatted(accessToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    private void signup(String email) throws Exception {
        String nickname = email.substring(0, email.indexOf('@'));
        SignupRequest request = new SignupRequest(
                email,
                "password123",
                nickname,
                nickname
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private String loginAndExtractRefreshToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("refreshToken").asText();
    }

    private String loginAndExtractAccessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }
}
