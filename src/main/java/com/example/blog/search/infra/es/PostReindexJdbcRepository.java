package com.example.blog.search.infra.es;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostReindexJdbcRepository { // 대량 재색인용인데 이제 안쓸듯 그냥 유물용

    private final JdbcTemplate jdbcTemplate;

    public List<ReindexSourceRow> fetchPublishedBatchAfterId(long lastPostId, int limit) {
        String sql = """
                SELECT
                    p.post_id,
                    p.title,
                    LEFT(COALESCE(p.content, ''), 200) AS content_preview,
                    p.author_id,
                    u.nickname AS author_nickname,
                    p.view_count,
                    p.like_count,
                    p.created_at,
                    p.post_status
                FROM post p
                JOIN `user` u ON u.user_id = p.author_id
                WHERE p.post_status = 'PUBLISHED'
                  AND p.deleted_at IS NULL
                  AND p.post_id > ?
                ORDER BY p.post_id ASC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("created_at");
            LocalDateTime createdAt = (ts == null) ? null : ts.toLocalDateTime();

            Long viewCount = rs.getObject("view_count") == null ? 0L : rs.getLong("view_count");
            Long likeCount = rs.getObject("like_count") == null ? 0L : rs.getLong("like_count");

            return new ReindexSourceRow(
                    rs.getLong("post_id"),
                    rs.getString("title"),
                    rs.getString("content_preview"),
                    rs.getLong("author_id"),
                    rs.getString("author_nickname"),
                    viewCount,
                    likeCount,
                    createdAt,
                    rs.getString("post_status")
            );
        }, lastPostId, limit);
    }

    public record ReindexSourceRow(
            Long postId,
            String title,
            String contentPreview,
            Long authorId,
            String authorNickname,
            Long viewCount,
            Long likeCount,
            LocalDateTime createdAt,
            String postStatus
    ) {}
}