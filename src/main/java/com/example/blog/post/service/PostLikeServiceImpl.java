package com.example.blog.post.service;

import com.example.blog.popular.service.PopularEventService;
import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostLike;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.presentation.dto.response.PostLikeToggleResponse;
import com.example.blog.post.repository.PostLikeRepository;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.global.exception.BlogException;
import com.example.blog.global.exception.ErrorCode;
import com.example.blog.search.application.port.PostSearchSyncPort;
import com.example.blog.user.entity.User;
import com.example.blog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeServiceImpl implements PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final PostSearchSyncPort postSearchSyncPort;
    private final PopularEventService popularEventService;

    @Override
    @Transactional
    public PostLikeToggleResponse toggleLike(Long postId, Long userId) {
        Post post = postRepository.findByPostIdAndPostStatusNot(postId, PostStatus.DELETED)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        Optional<PostLike> existingLike = postLikeRepository.findByPostPostIdAndUserUserId(postId, userId);

        boolean liked;
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            popularEventService.cancelLike(postId);
            post.decreaseLikeCount();
            liked = false;
        } else {
            User userRef = userRepository.getReferenceById(userId);

            PostLike postLike = new PostLike();
            postLike.setPost(post);
            postLike.setUser(userRef);

            postLikeRepository.save(postLike);
            popularEventService.reflectLike(postId);
            post.increaseLikeCount();
            liked = true;
        }

        post.increaseSyncVersion();

        // 네 기존 outbox -> scheduler -> ES 흐름에 맞게 enqueue만
        postSearchSyncPort.enqueuePostChanged(post);

        return PostLikeToggleResponse.of(liked, post.getLikeCount());
    }
}