package com.example.blog.search.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.post.presentation.dto.response.PostDetailDto;
import com.example.blog.search.presentation.dto.response.OffsetResponse;
import com.example.blog.search.presentation.dto.response.PostSummaryDto;
import com.example.blog.search.service.SearchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search", description = "게시글 검색 API")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // 기존 호환용 (기본은 infix)
    @GetMapping("/posts")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleInfix(keyword, offset, limit));
    }

    @GetMapping("/posts/infix")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsInfix(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleInfix(keyword, offset, limit));
    }

    @GetMapping("/posts/prefix")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsPrefix(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitlePrefix(keyword, offset, limit));
    }

    @GetMapping("/posts/fulltext")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsFulltext(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleFulltext(keyword, offset, limit));
    }

    @GetMapping("/posts/fulltext-boolean")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsFulltextBoolean(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleFulltextBoolean(keyword, offset, limit));
    }

    @GetMapping("/posts/es")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsEs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleEs(keyword, offset, limit));
    }
}
