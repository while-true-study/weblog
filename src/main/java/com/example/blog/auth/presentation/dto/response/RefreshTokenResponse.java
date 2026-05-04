package com.example.blog.auth.presentation.dto.response;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {}
