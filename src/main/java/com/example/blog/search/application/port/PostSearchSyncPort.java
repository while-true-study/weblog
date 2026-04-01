package com.example.blog.search.application.port;

import com.example.blog.post.entity.Post;

public interface PostSearchSyncPort {
    void enqueuePostChanged(Post post);
}