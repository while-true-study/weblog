package com.example.blog.search.mapper;

import com.example.blog.post.entity.Post;
import com.example.blog.search.infra.es.document.PostSearchDocument;
import com.example.blog.search.outbox.dto.PostOutboxPayload;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class PostSearchPayloadMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int PREVIEW_LENGTH = 150;

    public PostOutboxPayload toPayload(Post post) {
        return PostOutboxPayload.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .contentPreview(makePreview(post.getContent()))
                .authorId(post.getAuthor().getUserId())
                .authorNickname(post.getAuthor().getNickname())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .postStatus(post.getPostStatus() != null ? post.getPostStatus().name() : null)
                .version(post.getSyncVersion())
                .build();
    }

    private String makePreview(String content) {
        if (content == null) return "";
        String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, PREVIEW_LENGTH);
    }

    private PostSearchDocument toDocument(Post post) {
        PostSearchDocument doc = new PostSearchDocument();
        doc.setPostId(post.getPostId());
        doc.setTitle(post.getTitle());
        doc.setContentPreview(makePreview(post.getContent()));
        doc.setAuthorId(post.getAuthor() != null ? post.getAuthor().getUserId() : null);
        doc.setAuthorNickname(post.getAuthor() != null ? post.getAuthor().getNickname() : null);
        doc.setViewCount(post.getViewCount() != null ? post.getViewCount() : 0L);
        doc.setLikeCount(post.getLikeCount() != null ? post.getLikeCount() : 0L);
        doc.setCreatedAt(post.getCreatedAt() != null ? post.getCreatedAt() : null);
        doc.setPostStatus(post.getPostStatus() != null ? post.getPostStatus().name() : null);
        return doc;
    }
}