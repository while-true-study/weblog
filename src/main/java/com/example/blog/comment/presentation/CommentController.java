package com.example.blog.comment.presentation;

import com.example.blog.comment.presentation.dto.request.CommentCreateRequest;
import com.example.blog.comment.presentation.dto.request.CommentUpdateRequest;
import com.example.blog.comment.presentation.dto.response.CommentDeleteResponse;
import com.example.blog.comment.presentation.dto.response.CommentResponse;
import com.example.blog.comment.service.CommentService;
import com.example.blog.global.common.ApiResponse;
import com.example.blog.global.exception.BlogException;
import com.example.blog.global.exception.ErrorCode;
import com.example.blog.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment", description = "댓글 관련 API")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성", description = "특정 게시글에 댓글을 작성합니다. (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponse> createComment(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        if (userPrincipal == null) {
            throw new BlogException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(
                commentService.createComment(postId, userPrincipal.getId(), request)
        );
    }

    @Operation(summary = "댓글 목록 조회", description = "특정 게시글의 댓글 목록을 조회합니다. (로그인 시 본인 댓글 여부 포함)")
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getComments(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        Long loginUserId = userPrincipal != null ? userPrincipal.getId() : null;
        return ApiResponse.success(
                commentService.getComments(postId, loginUserId)
        );
    }

    @Operation(summary = "댓글 수정", description = "본인의 댓글을 수정합니다. (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        if (userPrincipal == null) {
            throw new BlogException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(
                commentService.updateComment(commentId, userPrincipal.getId(), request)
        );
    }

    @Operation(summary = "댓글 삭제", description = "본인의 댓글을 삭제합니다. (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<CommentDeleteResponse> deleteComment(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            throw new BlogException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(
                commentService.deleteComment(commentId, userPrincipal.getId())
        );
    }
}
