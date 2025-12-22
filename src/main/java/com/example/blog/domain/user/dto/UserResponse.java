package com.example.blog.domain.user.dto;

public record UserResponse(
        Long id,
        String email,
        String nickname
) {}