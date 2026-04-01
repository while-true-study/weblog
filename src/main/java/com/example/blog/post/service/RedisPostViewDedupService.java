package com.example.blog.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisPostViewDedupService implements PostViewDedupService {

    private static final Duration VIEW_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    @Override
    public boolean shouldIncrease(Long postId, String viewerId) {
        String key = "post:viewed:" + postId + ":" + viewerId;

        Boolean success = redis.opsForValue()
                .setIfAbsent(key, "1", VIEW_TTL);

        return Boolean.TRUE.equals(success);
    }
}