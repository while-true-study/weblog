package com.example.blog.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupRequest(
        @Schema(description = "이메일", example = "test@example.com")
        String email,

        @Schema(description = "비밀번호(서버에선 BCrypt로 저장)", example = "1234")
        String password,

        @Schema(description = "닉네임", example = "짱구는 목말라")
        String nickname,

        @Schema(description = "유저 이름", example = "홍길동")
        String username
) {}