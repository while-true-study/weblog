package com.example.blog.post.presentation.dto.response;

import com.example.blog.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostDetailDto(
        Long id,
        String title,
        String content,
        AuthorDto author,
        String category,
        List<String> tags,
        long viewCount,
        long likeCount,
        LocalDateTime createdAt,
        LocalDateTime updateAt
) {
    public static PostDetailDto from(Post p) {
        return new PostDetailDto(
                p.getPostId(),
                p.getTitle(),
                p.getContent(),
                AuthorDto.from(p.getAuthor()),
                p.getCategory().getCategoriesName(),
                p.getPostTags().stream().map(pt -> pt.getTag().getTagName()).toList(),
                p.getViewCount(),
                p.getLikeCount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
