package com.example.blog.search.repository;

import java.time.LocalDateTime;
import java.util.List;

public interface PostTitleSearchCustomRepository {

    FulltextSliceResult searchTitleFulltextNative(String keyword, int offset, int limit);

    FulltextSliceResult searchTitleFulltextBooleanNative(String keyword, int offset, int limit);

    record FulltextRow(
            Long postId,
            String title,
            String contentPreview,
            Long authorId,
            String authorNickname,
            Long viewCount,
            Long likeCount,
            LocalDateTime createdAt
    ) {}

    record FulltextSliceResult(
            List<FulltextRow> items,
            boolean hasNext
    ) {}
}