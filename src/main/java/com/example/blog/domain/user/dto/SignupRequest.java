package com.example.blog.domain.user.dto;

public record SignupRequest(
        String email,
        String password,
        String nickname,
        String username
) {}