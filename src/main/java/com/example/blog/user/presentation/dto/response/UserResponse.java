package com.example.blog.user.presentation.dto.response;

public record UserResponse(
        Long id,
        String email,
        String nickname
) {}