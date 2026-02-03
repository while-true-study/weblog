package com.example.blog.post.repository;

import com.example.blog.post.entity.PostViewCounterShard;
import com.example.blog.post.entity.PostViewCounterShardId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostViewCounterShardRepository extends JpaRepository<PostViewCounterShard, PostViewCounterShardId> {

    @Modifying
    @Query(value = """
INSERT INTO post_view_counter_shard (post_id, shard_id, view_count)
VALUES (:postId, :shardId, 1)
ON DUPLICATE KEY UPDATE view_count = view_count + 1
""", nativeQuery = true)
    int incrementUpsert(@Param("postId") Long postId, @Param("shardId") int shardId);

    @Query(value = """
        SELECT COALESCE(SUM(view_count), 0)
        FROM post_view_counter_shard
        WHERE post_id = :postId
        """, nativeQuery = true)
    long sumByPostId(@Param("postId") Long postId); // 조회수 합치기
}