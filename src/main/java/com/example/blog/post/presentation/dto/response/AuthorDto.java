package com.example.blog.post.presentation.dto.response;

import com.example.blog.user.entity.User;

public record AuthorDto(Long id, String nickname) {
    public static AuthorDto from(User u) {
        return new AuthorDto(u.getUserId(), u.getNickname());
    }
}
