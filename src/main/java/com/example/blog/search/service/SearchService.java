package com.example.blog.search.service;

import com.example.blog.post.entity.Post;
import com.example.blog.search.infra.es.EsSearchSliceResult;
import com.example.blog.search.infra.es.PostSearchEsRepository;
import com.example.blog.search.infra.es.document.PostSearchDocument;
import com.example.blog.search.presentation.dto.response.OffsetResponse;
import com.example.blog.search.presentation.dto.response.PostSummaryDto;
import com.example.blog.search.repository.PostTitleSearchCustomRepository;
import com.example.blog.search.repository.PostTitleSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final PostTitleSearchRepository postTitleSearchRepository;
    private final PostSearchEsRepository postSearchEsRepository;

    public OffsetResponse<PostSummaryDto> searchPostsByTitleInfix(String keyword, int offset, int limit) {
        return searchPostsByTitleInternal(keyword, offset, limit, "infix");
    }

    public OffsetResponse<PostSummaryDto> searchPostsByTitlePrefix(String keyword, int offset, int limit) {
        return searchPostsByTitleInternal(keyword, offset, limit, "prefix");
    }

    public OffsetResponse<PostSummaryDto> searchPostsByTitleFulltext(String keyword, int offset, int limit) {
        return searchPostsByTitleInternal(keyword, offset, limit, "fulltext");
    }

    public OffsetResponse<PostSummaryDto> searchPostsByTitleFulltextBoolean(String keyword, int offset, int limit) {
        return searchPostsByTitleInternal(keyword, offset, limit, "fulltext_boolean");
    }

    private OffsetResponse<PostSummaryDto> searchPostsByTitleInternal(String keyword, int offset, int limit, String mode) {
        String k = keyword == null ? "" : keyword.trim();
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        if (k.isBlank()) {
            return OffsetResponse.of(List.of(), safeOffset, safeLimit, false);
        }

        String normalizedMode = (mode == null) ? "infix" : mode.trim().toLowerCase();

        // ===== fulltext 계열: custom repo (real offset + limit+1) =====
        if ("fulltext".equals(normalizedMode)) {
            PostTitleSearchCustomRepository.FulltextSliceResult result =
                    postTitleSearchRepository.searchTitleFulltextNative(k, safeOffset, safeLimit);

            List<PostSummaryDto> items = result.items().stream()
                    .map(this::toDtoFromFulltextRow)
                    .toList();

            return OffsetResponse.of(items, safeOffset, safeLimit, result.hasNext());
        }

        if ("fulltext_boolean".equals(normalizedMode)) {
            PostTitleSearchCustomRepository.FulltextSliceResult result =
                    postTitleSearchRepository.searchTitleFulltextBooleanNative(k, safeOffset, safeLimit);

            List<PostSummaryDto> items = result.items().stream()
                    .map(this::toDtoFromFulltextRow)
                    .toList();

            return OffsetResponse.of(items, safeOffset, safeLimit, result.hasNext());
        }

        // ===== like 계열: 기존 JPA Slice =====
        int page = safeOffset / safeLimit;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "postId"));

        Pageable pageableForLike = PageRequest.of(page, safeLimit, sort);

        Slice<Post> slice = "prefix".equals(normalizedMode)
                ? postTitleSearchRepository.searchTitlePrefix(k, pageableForLike)
                : postTitleSearchRepository.searchTitleInfix(k, pageableForLike);

        List<PostSummaryDto> items = slice.getContent().stream()
                .map(this::toDto)
                .toList();

        return OffsetResponse.of(items, safeOffset, safeLimit, slice.hasNext());
    }

    private PostSummaryDto toDto(Post post) {
        return new PostSummaryDto(
                post.getPostId(),
                post.getTitle(),
                post.getContent(), // like 계열은 기존 유지
                new PostSummaryDto.AuthorDto(
                        post.getAuthor().getUserId(),
                        post.getAuthor().getNickname()
                ),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCreatedAt()
        );
    }

    private PostSummaryDto toDtoFromFulltextRow(PostTitleSearchCustomRepository.FulltextRow row) {
        return new PostSummaryDto(
                row.postId(),
                row.title(),
                row.contentPreview(), // 성능 위해 preview만
                new PostSummaryDto.AuthorDto(
                        row.authorId(),
                        row.authorNickname()
                ),
                row.viewCount(),
                row.likeCount(),
                row.createdAt()
        );
    }



    public OffsetResponse<PostSummaryDto> searchPostsByTitleEs(String keyword, int offset, int limit) {
        String k = keyword == null ? "" : keyword.trim();
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        if (k.isBlank()) {
            return OffsetResponse.of(List.of(), safeOffset, safeLimit, false);
        }

        EsSearchSliceResult result = postSearchEsRepository.searchTitle(k, safeOffset, safeLimit);

        List<PostSummaryDto> items = result.getItems().stream()
                .map(this::toPostSummaryDtoFromEs)
                .toList();

        return OffsetResponse.of(items, safeOffset, safeLimit, result.isHasNext());
    }

    private PostSummaryDto toPostSummaryDtoFromEs(PostSearchDocument doc) {
        return new PostSummaryDto(
                doc.getPostId(),
                doc.getTitle(),
                doc.getContent() == null ? "" : doc.getContent(),
                new PostSummaryDto.AuthorDto(
                        doc.getAuthorId(),
                        doc.getAuthorNickname()
                ),
                doc.getViewCount(),
                doc.getLikeCount(),
                doc.getCreatedAt()
        );
    }


    private LocalDateTime parseCreatedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value)
                    .atZone(ZoneId.of("Asia/Seoul")) // 원하면 시스템/한국시간으로 변환
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        // 혹시 로컬 형식으로 들어오는 경우 대응 (예: "2026-02-25T09:52:02")
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("createdAt 파싱 실패: " + value, e);
        }
    }
}