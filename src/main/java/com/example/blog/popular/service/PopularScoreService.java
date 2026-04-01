package com.example.blog.popular.service;

import com.example.blog.popular.PopularKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PopularScoreService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration DAILY_TTL = Duration.ofDays(8);
    private static final Duration WEEKLY_TTL = Duration.ofDays(35);

    public void increaseScore(Long postId, double score) {
        LocalDate today = LocalDate.now();

        String dailyKey = PopularKeyGenerator.dailyKey(today);
        String weeklyKey = PopularKeyGenerator.weeklyKey(today);
        String member = String.valueOf(postId);

        redisTemplate.opsForZSet().incrementScore(dailyKey, member, score);
        redisTemplate.opsForZSet().incrementScore(weeklyKey, member, score);

        redisTemplate.expire(dailyKey, DAILY_TTL);
        redisTemplate.expire(weeklyKey, WEEKLY_TTL);
    }

    public void decreaseScore(Long postId, double score) {
        increaseScore(postId, -score);
    }
}