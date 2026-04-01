package com.example.blog.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPostRankingCache implements PostRankingCache {

    private static final String RANK_ZSET = "post:rank";

    private final StringRedisTemplate redis;

    @Override
    public void increaseViewScore(Long postId) {
        redis.opsForZSet().incrementScore(RANK_ZSET, postId.toString(), 1);
    }

    @Override
    public void increaseLikeScore(Long postId) {
        redis.opsForZSet().incrementScore(RANK_ZSET, postId.toString(), 3);
    }
}