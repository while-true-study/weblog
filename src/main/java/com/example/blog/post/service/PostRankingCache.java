package com.example.blog.post.service;

public interface PostRankingCache {
    void increaseViewScore(Long postId);
    void increaseLikeScore(Long postId);
}