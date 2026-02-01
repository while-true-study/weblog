package com.example.blog.post.service;

import com.example.blog.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewCountService {
    private final PostRepository postRepository;

    @Transactional // 또는 REQUIRES_NEW (의미를 더 강하게 주고 싶으면)
    public void increment(Long postId) {
        postRepository.incrementViewCount(postId);
    }
}