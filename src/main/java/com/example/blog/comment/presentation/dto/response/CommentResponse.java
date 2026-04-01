package com.example.blog.comment.presentation.dto.response;

import com.example.blog.comment.entity.Comment;

public record CommentResponse(
        Long id,
        Long postId,
        CommentAuthorDto author,
        String content,
        boolean deleted,
        String createdAt,
        String updatedAt,
        boolean mine
) {
    public static CommentResponse from(Comment comment, Long loginUserId) {
        boolean mine = loginUserId != null && comment.getAuthor().getUserId().equals(loginUserId);

        return new CommentResponse(
                comment.getId(),
                comment.getPost().getPostId(),
                new CommentAuthorDto(
                        comment.getAuthor().getUserId(),
                        comment.getAuthor().getNickname(),
                        comment.getAuthor().toString()
                ),
                comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent(),
                comment.isDeleted(),
                comment.getCreatedAt().toString(),
                comment.getUpdatedAt().toString(),
                mine
        );
    }
}
