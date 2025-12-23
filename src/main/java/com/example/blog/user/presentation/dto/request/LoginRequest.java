package com.example.blog.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(description = "로그인 이메일", example = "test123@example.com")
        String email,

        @Schema(description = "비밀번호", example = "1234567890")
        String password
) {}