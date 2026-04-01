package com.example.blog.post.service;

public interface PostViewDedupService {
    boolean shouldIncrease(Long postId, String viewerId);
}