package com.example.blog.post.presentation.dto.response;

import com.example.blog.post.entity.Post;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

public record PostCreateResponse (
        Long id,
        String title,
        String status,
        LocalDateTime createdAt
) {
    public static PostCreateResponse from(Post p) {
        return new PostCreateResponse(
                p.getPostId(),
                p.getTitle(),
                p.getPostStatus().name(),
                p.getCreatedAt()
        );
    }
}
