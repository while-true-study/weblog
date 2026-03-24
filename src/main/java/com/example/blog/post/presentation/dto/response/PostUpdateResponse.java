package com.example.blog.post.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostUpdateResponse {
    private Long postId;
    private String title;
    private Long version;
    private LocalDateTime updatedAt;
}