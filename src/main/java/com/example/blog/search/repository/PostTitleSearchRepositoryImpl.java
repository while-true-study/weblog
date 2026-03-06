package com.example.blog.search.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostTitleSearchRepositoryImpl implements PostTitleSearchCustomRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<FulltextRow> fulltextRowMapper = (rs, rowNum) -> {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (ts != null) ? ts.toLocalDateTime() : null;

        return new FulltextRow(
                rs.getLong("post_id"),
                rs.getString("title"),
                rs.getString("content_preview"),
                rs.getLong("author_id"),
                rs.getString("author_nickname"),
                rs.getLong("view_count"),
                rs.getLong("like_count"),
                createdAt
        );
    };

    @Override
    public FulltextSliceResult searchTitleFulltextNative(String keyword, int offset, int limit) {
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(limit, 1);
        int fetchSize = safeLimit + 1; // hasNext 계산용

        String sql = """
                SELECT
                    p.post_id,
                    p.title,
                    LEFT(COALESCE(p.content, ''), 200) AS content_preview,
                    p.author_id,
                    u.nickname AS author_nickname,
                    p.view_count,
                    p.like_count,
                    p.created_at
                FROM post p
                JOIN `user` u ON u.user_id = p.author_id
                WHERE p.post_status = 'PUBLISHED'
                  AND p.deleted_at IS NULL
                  AND MATCH(p.title) AGAINST (? IN NATURAL LANGUAGE MODE)
                ORDER BY p.created_at DESC, p.post_id DESC
                LIMIT ? OFFSET ?
                """;

        List<FulltextRow> rows = jdbcTemplate.query(
                sql,
                fulltextRowMapper,
                keyword,
                fetchSize,
                safeOffset
        );

        boolean hasNext = rows.size() > safeLimit;
        if (hasNext) {
            rows = rows.subList(0, safeLimit);
        }

        return new FulltextSliceResult(rows, hasNext);
    }

    @Override
    public FulltextSliceResult searchTitleFulltextBooleanNative(String keyword, int offset, int limit) {
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(limit, 1);
        int fetchSize = safeLimit + 1; // hasNext 계산용

        String sql = """
                SELECT
                    p.post_id,
                    p.title,
                    LEFT(COALESCE(p.content, ''), 200) AS content_preview,
                    p.author_id,
                    u.nickname AS author_nickname,
                    p.view_count,
                    p.like_count,
                    p.created_at
                FROM post p
                JOIN `user` u ON u.user_id = p.author_id
                WHERE p.post_status = 'PUBLISHED'
                  AND p.deleted_at IS NULL
                  AND MATCH(p.title) AGAINST (CONCAT(?, '*') IN BOOLEAN MODE)
                ORDER BY p.created_at DESC, p.post_id DESC
                LIMIT ? OFFSET ?
                """;

        List<FulltextRow> rows = jdbcTemplate.query(
                sql,
                fulltextRowMapper,
                keyword,
                fetchSize,
                safeOffset
        );

        boolean hasNext = rows.size() > safeLimit;
        if (hasNext) {
            rows = rows.subList(0, safeLimit);
        }

        return new FulltextSliceResult(rows, hasNext);
    }
}