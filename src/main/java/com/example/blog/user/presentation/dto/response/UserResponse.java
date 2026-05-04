package com.example.blog.user.presentation.dto.response;

import com.example.blog.user.entity.CustomUserPrincipal;
import com.example.blog.user.entity.User;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String username,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    public static UserResponse from(CustomUserPrincipal principal) {
        return new UserResponse(
                principal.getId(),
                principal.getEmail(),
                principal.getNickname(),
                principal.getDisplayUsername(),
                principal.getRole()
        );
    }
}
