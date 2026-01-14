package com.example.blog.post.service;

import com.example.blog.post.presentation.dto.request.PostPublishedDto;
import com.example.blog.post.presentation.dto.response.PostCreateResponse;
import com.example.blog.post.presentation.dto.response.PostDetailDto;
import com.example.blog.post.presentation.dto.response.PostListItemDto;
import com.example.blog.post.presentation.dto.response.PostSearchCond;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {
    Page<PostListItemDto> getPosts(PostSearchCond cond, Pageable pageable);

    PostDetailDto getPost(Long id);

    PostCreateResponse createPost(PostPublishedDto postPublishedDto);
}