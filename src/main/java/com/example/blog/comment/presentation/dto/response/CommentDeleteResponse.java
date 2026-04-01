package com.example.blog.comment.presentation.dto.response;

public record CommentDeleteResponse(
        Long commentId,
        boolean deleted
) {
    public static CommentDeleteResponse of(Long commentId) {
        return new CommentDeleteResponse(commentId, true);
    }
}