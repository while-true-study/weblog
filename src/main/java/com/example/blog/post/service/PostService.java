package com.example.blog.post.service;

import com.example.blog.post.presentation.dto.request.PostPublishedDto;
import com.example.blog.post.presentation.dto.request.PostUpdateRequest;
import com.example.blog.post.presentation.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    Page<PostListItemDto> getPosts(PostSearchCond cond, Pageable pageable);

    PostDetailDto getPost(Long id);

    PostCreateResponse createPost(PostPublishedDto postPublishedDto, String username);

    PostUpdateResponse updatePost(Long postId, PostUpdateRequest req, String email);

    void deletePost(Long postId, String email);
}