package com.example.blog.user.presentation.dto.request;

public record LoginRequest(
        String email,
        String password
) {}