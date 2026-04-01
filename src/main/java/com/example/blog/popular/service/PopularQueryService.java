package com.example.blog.popular.service;

import com.example.blog.popular.PopularKeyGenerator;
import com.example.blog.popular.presentation.dto.response.PopularPostResponse;
import com.example.blog.post.entity.Post;
import com.example.blog.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PopularQueryService {

    private final StringRedisTemplate redisTemplate;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public List<PopularPostResponse> getDailyPopularPosts(LocalDate date, int limit) {
        String key = PopularKeyGenerator.dailyKey(date);

        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> postIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .toList();

        Map<Long, Double> scoreMap = tuples.stream()
                .filter(t -> t.getValue() != null && t.getScore() != null)
                .collect(Collectors.toMap(
                        t -> Long.valueOf(t.getValue()),
                        ZSetOperations.TypedTuple::getScore
                ));

        List<Post> posts = postRepository.findAllById(postIds);
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getPostId, p -> p));

        List<PopularPostResponse> result = new ArrayList<>();
        int rank = 1;

        for (Long postId : postIds) {
            Post post = postMap.get(postId);
            if (post == null) {
                continue;
            }

            result.add(PopularPostResponse.builder()
                    .postId(post.getPostId())
                    .title(post.getTitle())
                    .summary(post.getContent())
                    .authorNickname(post.getAuthor().getNickname())
                    .likeCount(post.getLikeCount())
                    .viewCount(post.getViewCount())
                    .popularScore(scoreMap.get(postId))
                    .rank(rank++)
                    .build());
        }

        return result;
    }
}