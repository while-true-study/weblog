package com.example.blog.post.service;

import com.example.blog.post.presentation.dto.response.PostLikeToggleResponse;

public interface PostLikeService {
    PostLikeToggleResponse toggleLike(Long postId, Long userId);
}
