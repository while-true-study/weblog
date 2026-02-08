package com.example.blog.search.presentation.dto.response;

import java.time.LocalDateTime;

public record PostSummaryDto(
        Long id,
        String title,
        String summary,
        AuthorDto author,
        Long viewCount,
        Long likeCount,
        LocalDateTime createdAt
) {
    public record AuthorDto(Long id, String nickname) {}
}
