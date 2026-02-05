package com.example.blog.post.service;

public interface ViewCounter {
    void increment(Long postId);
    long getViewCount(Long postId);
}
