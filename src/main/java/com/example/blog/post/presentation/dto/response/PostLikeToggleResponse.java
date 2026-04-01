package com.example.blog.post.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class PostLikeToggleResponse {
    private boolean liked;
    private Long likeCount;
}
