package com.example.blog.comment.presentation.dto.response;

public record CommentAuthorDto(
        Long id,
        String nickname,
        String avatar
) {
}
