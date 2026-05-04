package com.example.blog.user.presentation.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
