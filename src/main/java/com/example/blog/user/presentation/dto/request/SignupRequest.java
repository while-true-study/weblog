package com.example.blog.user.presentation.dto.request;

public record SignupRequest(
        String email,
        String password,
        String nickname,
        String username
) {}