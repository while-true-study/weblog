package com.example.blog.post.presentation.dto.response;

import com.example.blog.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostListItemDto(
        Long id,
        String title,
        String summary,
        AuthorDto author,
        List<String> tags,
        long viewCount,
        long likeCount,
        LocalDateTime createdAt
) {
    public static PostListItemDto from(Post p) {
        return new PostListItemDto(
                p.getPostId(),
                p.getTitle(),
                p.getContent(),
                new AuthorDto(p.getAuthor().getUserId(), p.getAuthor().getNickname()),
                p.getPostTags().stream().map(pt -> pt.getTag().getTagName()).toList(),
                p.getViewCount(),
                p.getLikeCount(),
                p.getCreatedAt()
        );
    }

    public record AuthorDto(Long id, String nickname) {}
}

