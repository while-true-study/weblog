package com.example.blog.post.presentation.dto.response;

import com.example.blog.post.entity.Post;
import com.example.blog.series.entity.Series;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailDto(
        Long id,
        String title,
        String content,
        AuthorDto author,

        Long seriesId,
        String seriesName,

        List<String> tags,
        long viewCount,
        long likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailDto from(Post p, long viewCount) {
        Long seriesId = (p.getSeries() != null) ? p.getSeries().getId() : null;
        String seriesName = (p.getSeries() != null) ? p.getSeries().getName() : null;

        return new PostDetailDto(
                p.getPostId(),
                p.getTitle(),
                p.getContent(),
                AuthorDto.from(p.getAuthor()),

                seriesId,
                seriesName,

                p.getPostTags().stream()
                        .map(pt -> pt.getTag().getTagName())
                        .toList(),
                viewCount,
                p.getLikeCount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
