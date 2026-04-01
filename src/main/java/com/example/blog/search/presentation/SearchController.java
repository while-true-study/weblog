package com.example.blog.search.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.post.presentation.dto.response.PostDetailDto;
import com.example.blog.search.presentation.dto.response.OffsetResponse;
import com.example.blog.search.presentation.dto.response.PostSummaryDto;
import com.example.blog.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "게시글 검색 (infix, 기본)", description = "제목 infix 검색입니다. 기존 호환용 엔드포인트.")
    @GetMapping("/posts")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPosts(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "오프셋 (기본값 0)") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "페이지 크기 (기본값 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleInfix(keyword, offset, limit));
    }

    @Operation(summary = "게시글 검색 (infix)", description = "제목 infix 방식으로 검색합니다.")
    @GetMapping("/posts/infix")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsInfix(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "오프셋 (기본값 0)") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "페이지 크기 (기본값 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleInfix(keyword, offset, limit));
    }

    @Operation(summary = "게시글 검색 (prefix)", description = "제목 prefix 방식으로 검색합니다.")
    @GetMapping("/posts/prefix")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsPrefix(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "오프셋 (기본값 0)") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "페이지 크기 (기본값 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitlePrefix(keyword, offset, limit));
    }

    @Operation(summary = "게시글 검색 (fulltext)", description = "제목 fulltext 방식으로 검색합니다.")
    @GetMapping("/posts/fulltext")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsFulltext(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "오프셋 (기본값 0)") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "페이지 크기 (기본값 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleFulltext(keyword, offset, limit));
    }

    @Operation(summary = "게시글 검색 (fulltext-boolean)", description = "제목 fulltext boolean 방식으로 검색합니다.")
    @GetMapping("/posts/fulltext-boolean")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsFulltextBoolean(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "오프셋 (기본값 0)") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "페이지 크기 (기본값 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleFulltextBoolean(keyword, offset, limit));
    }

    @Operation(summary = "게시글 검색 (Elasticsearch)", description = "Elasticsearch를 통해 제목으로 검색합니다.")
    @GetMapping("/posts/es")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPostsEs(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @Parameter(description = "오프셋 (기본값 0)") @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "페이지 크기 (기본값 20)") @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitleEs(keyword, offset, limit));
    }
}
