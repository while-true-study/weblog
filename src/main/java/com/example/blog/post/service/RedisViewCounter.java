package com.example.blog.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisViewCounter implements ViewCounter{

    private final StringRedisTemplate redis;

    private static final String VIEW_HASH = "post:view";
    private static final String RANK_ZSET = "post:rank";

    @Override
    public void increment(Long postId) {
        String pid = postId.toString();
        redis.opsForHash().increment(VIEW_HASH, pid, 1);      // HINCRBY pid마다 카운트
        redis.opsForZSet().incrementScore(RANK_ZSET, pid, 1); // ZINCRBY pid마다 점수
    }

    @Override
    public long getViewCount(Long postId) {
        Object v = redis.opsForHash().get(VIEW_HASH, postId.toString()); // 가져오기
        return (v == null) ? 0L : Long.parseLong(v.toString());
    }
}
