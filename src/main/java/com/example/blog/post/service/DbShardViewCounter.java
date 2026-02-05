package com.example.blog.post.service;

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
public class DbShardViewCounter{

    private static final int SHARD_COUNT = 8;

    private final PostViewCounterShardRepository repo;
    private final JdbcTemplate jdbcTemplate;

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