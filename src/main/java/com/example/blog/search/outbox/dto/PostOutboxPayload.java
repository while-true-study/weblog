package com.example.blog.search.outbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostOutboxPayload { //
    private Long postId;
    private String title;
    private String contentPreview;
    private Long authorId;
    private String authorNickname;
    private Long viewCount;
    private Long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String postStatus;
    private Long version;
}
