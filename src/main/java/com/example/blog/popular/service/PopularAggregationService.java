package com.example.blog.popular.service;

import com.example.blog.popular.PopularKeyGenerator;
import com.example.blog.popular.entity.PopularPostDaily;
import com.example.blog.popular.repository.PopularPostDailyRepository;
import com.example.blog.post.entity.Post;
import com.example.blog.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PopularAggregationService {

    private final StringRedisTemplate redisTemplate;
    private final PopularPostDailyRepository popularPostDailyRepository;
    private final PostRepository postRepository;

    @Transactional
    public void aggregateDaily(LocalDate targetDate, int limit) {
        String key = PopularKeyGenerator.dailyKey(targetDate);

        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return;
        }

        popularPostDailyRepository.deleteByTargetDate(targetDate);

        List<PopularPostDaily> items = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }

            Long postId = Long.valueOf(tuple.getValue());
            Post post = postRepository.findById(postId)
                    .orElse(null);

            if (post == null) {
                continue;
            }

            items.add(PopularPostDaily.builder()
                    .targetDate(targetDate)
                    .post(post)
                    .rankNo(rank++)
                    .score(tuple.getScore())
                    .build());
        }

        popularPostDailyRepository.saveAll(items);
    }
}