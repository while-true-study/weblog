package com.example.blog.post.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.post.presentation.dto.request.PostPublishedDto;
import com.example.blog.post.presentation.dto.request.PostUpdateRequest;
import com.example.blog.post.presentation.dto.response.PostCreateResponse;
import com.example.blog.post.presentation.dto.response.PostDetailDto;
import com.example.blog.post.presentation.dto.response.PostListItemDto;
import com.example.blog.post.presentation.dto.response.PostSearchCond;
import com.example.blog.post.presentation.dto.response.PostUpdateResponse;
import com.example.blog.post.presentation.dto.response.SliceResponse;
import com.example.blog.post.service.PostService;
import com.example.blog.post.service.PostViewerIdResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Post", description = "게시글 관련 API")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostViewerIdResolver postViewerIdResolver;

    @Operation(summary = "게시글 목록 조회", description = "검색 조건과 페이징으로 게시글 목록을 조회합니다.")
    @GetMapping
    public SliceResponse<PostListItemDto> getPosts(PostSearchCond cond, Pageable pageable) {
        Slice<PostListItemDto> result = postService.getPosts(cond, pageable);
        return SliceResponse.from(result);
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 ID로 상세 내용을 조회합니다. 조회수가 중복 없이 증가합니다.")
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailDto> getPostDetail(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            HttpServletRequest request
    ) {
        String viewerId = postViewerIdResolver.resolve(request);
        return ApiResponse.success(postService.getPost(postId, viewerId));
    }

    @Operation(summary = "게시글 작성", description = "새 게시글을 작성합니다. (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ApiResponse<PostCreateResponse> createPost(
            @RequestBody PostPublishedDto req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PostCreateResponse postCreateResponse = postService.createPost(req, userDetails.getUsername());
        return ApiResponse.success(postCreateResponse);
    }

    @Operation(summary = "게시글 수정", description = "본인의 게시글을 수정합니다. (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{postId}")
    public ApiResponse<PostUpdateResponse> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @RequestBody PostUpdateRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ApiResponse.success(
                postService.updatePost(postId, req, userDetails.getUsername())
        );
    }

    @Operation(summary = "게시글 삭제", description = "본인의 게시글을 삭제합니다. (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        postService.deletePost(postId, userDetails.getUsername());
        return ApiResponse.success(null);
    }
}
