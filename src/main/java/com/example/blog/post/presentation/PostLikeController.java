package com.example.blog.post.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.post.presentation.dto.response.PostLikeToggleResponse;
import com.example.blog.post.service.PostLikeService;
import com.example.blog.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "PostLike", description = "게시글 좋아요 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @Operation(summary = "게시글 좋아요 토글", description = "게시글 좋아요를 추가하거나 취소합니다. (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<PostLikeToggleResponse>> toggleLike(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        PostLikeToggleResponse response = postLikeService.toggleLike(postId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}