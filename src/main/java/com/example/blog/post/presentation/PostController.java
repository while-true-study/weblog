package com.example.blog.post.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.post.presentation.dto.response.PageResponse;
import com.example.blog.post.presentation.dto.response.PostDetailDto;
import com.example.blog.post.presentation.dto.response.PostListItemDto;
import com.example.blog.post.presentation.dto.response.PostSearchCond;
import com.example.blog.post.service.PostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Post", description = "게시글 관련 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<PageResponse<PostListItemDto>> getPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PostSearchCond cond = new PostSearchCond(keyword, categoryId, tag);
        Page<PostListItemDto> page = postService.getPosts(cond, pageable);
        return ApiResponse.success(PageResponse.from(page));
    }

    @GetMapping("/{postId}")
    public ApiResponse<PostDetailDto> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.success(postService.getPost(postId));
    }
}
