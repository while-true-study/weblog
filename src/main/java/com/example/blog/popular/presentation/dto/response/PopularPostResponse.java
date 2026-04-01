package com.example.blog.popular.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopularPostResponse {
    private Long postId;
    private String title;
    private String summary;
    private String authorNickname;
    private Long likeCount;
    private Long viewCount;
    private Double popularScore;
    private Integer rank;
}