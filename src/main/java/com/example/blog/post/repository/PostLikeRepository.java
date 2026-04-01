package com.example.blog.post.repository;

import com.example.blog.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostPostIdAndUserUserId(Long postId, Long userId);
}
