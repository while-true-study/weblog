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

@Tag(name = "Search", description = "게시글 관련 API")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/posts")
    public ApiResponse<OffsetResponse<PostSummaryDto>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(searchService.searchPostsByTitle(keyword, offset, limit, "infix"));
    }
}
