package com.example.blog.search.service;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.search.presentation.dto.response.OffsetResponse;
import com.example.blog.search.presentation.dto.response.PostSummaryDto;
import com.example.blog.search.repository.PostTitleSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PostTitleSearchRepository postTitleSearchRepository;

    public OffsetResponse<PostSummaryDto> searchPostsByTitle(String keyword, int offset, int limit, String mode) {
        String k = keyword == null ? "" : keyword.trim();
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        if (k.isBlank()) {
            return OffsetResponse.of(List.of(), safeOffset, safeLimit, false);
        }

        int page = safeOffset / safeLimit;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "postId"));

        Pageable pageable = PageRequest.of(page, safeLimit, sort);

        Slice<Post> slice = "prefix".equalsIgnoreCase(mode)
                ? postTitleSearchRepository.searchTitlePrefix(k, pageable)
                : postTitleSearchRepository.searchTitleInfix(k, pageable);

        List<PostSummaryDto> items = slice.getContent().stream()
                .map(this::toDto)
                .toList();

        return OffsetResponse.of(items, safeOffset, safeLimit, slice.hasNext());
    }

    private PostSummaryDto toDto(Post post) { // Dto로 바꾸기
        return new PostSummaryDto(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                new PostSummaryDto.AuthorDto(
                        post.getAuthor().getUserId(),
                        post.getAuthor().getNickname()
                ),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCreatedAt()
        );
    }
}
