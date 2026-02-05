package com.example.blog.post.service;

import com.example.blog.post.repository.PostRepository;
import com.example.blog.post.repository.PostViewCounterShardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class ViewCountService {
//    private final PostRepository postRepository;
//    @Transactional // 또는 REQUIRES_NEW (의미를 더 강하게 주고 싶으면)
//    public void increment(Long postId) {
//        postRepository.incrementViewCount(postId);
//    }

    private static final int SHARD_COUNT = 8;

    private final PostViewCounterShardRepository repo;


    // 샤딩 추가
    @Transactional
    public void incrementView(Long postId) {
        int shardId = ThreadLocalRandom.current().nextInt(SHARD_COUNT);
        repo.incrementUpsert(postId, shardId);
    }

    @Transactional(readOnly = true)
    public long getTotalView(Long postId) {
        return repo.sumByPostId(postId);
    }

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.MANDATORY)
    public void initShards(Long postId) {
        String sql = "INSERT INTO post_view_counter_shard(post_id, shard_id, view_count) VALUES (?, ?, 0)";
        List<Object[]> batchArgs = new ArrayList<>(SHARD_COUNT);
        for (int i = 0; i < SHARD_COUNT; i++) {
            batchArgs.add(new Object[]{postId, i});
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}