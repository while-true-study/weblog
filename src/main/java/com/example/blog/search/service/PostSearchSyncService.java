package com.example.blog.search.service;

import com.example.blog.search.outbox.dto.PostOutboxPayload;

public interface PostSearchSyncService {
    void upsert(PostOutboxPayload payload);
    void delete(Long postId, Long version);
}