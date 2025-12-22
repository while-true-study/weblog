package com.example.blog.domain.user.dto;

public record LoginRequest(
        String email,
        String password
) {}