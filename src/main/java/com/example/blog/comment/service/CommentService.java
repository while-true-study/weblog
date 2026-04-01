package com.example.blog.comment.service;

import com.example.blog.comment.presentation.dto.request.CommentCreateRequest;
import com.example.blog.comment.presentation.dto.request.CommentUpdateRequest;
import com.example.blog.comment.presentation.dto.response.CommentDeleteResponse;
import com.example.blog.comment.presentation.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse createComment(Long postId, Long loginUserId, CommentCreateRequest request);

    List<CommentResponse> getComments(Long postId, Long loginUserId);

    CommentResponse updateComment(Long commentId, Long loginUserId, CommentUpdateRequest request);

    CommentDeleteResponse deleteComment(Long commentId, Long loginUserId);
}