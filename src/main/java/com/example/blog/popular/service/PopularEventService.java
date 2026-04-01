package com.example.blog.popular.service;

import com.example.blog.popular.PopularScorePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PopularEventService {

    private final PopularScoreService popularScoreService;

    public void reflectView(Long postId) {
        popularScoreService.increaseScore(postId, PopularScorePolicy.VIEW_SCORE);
    }

    public void reflectLike(Long postId) {
        popularScoreService.increaseScore(postId, PopularScorePolicy.LIKE_SCORE);
    }

    public void cancelLike(Long postId) {
        popularScoreService.decreaseScore(postId, PopularScorePolicy.LIKE_SCORE);
    }

    public void reflectComment(Long postId) {
        popularScoreService.increaseScore(postId, PopularScorePolicy.COMMENT_SCORE);
    }

    public void cancelComment(Long postId) {
        popularScoreService.decreaseScore(postId, PopularScorePolicy.COMMENT_SCORE);
    }
}