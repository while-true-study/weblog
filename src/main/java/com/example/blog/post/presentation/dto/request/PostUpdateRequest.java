package com.example.blog.post.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PostUpdateRequest {
    private String title;
    private String content;
    private Long seriesId;
    private List<String> tags;
    private String status;
}
