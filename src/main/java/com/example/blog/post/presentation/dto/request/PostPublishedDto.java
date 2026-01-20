package com.example.blog.post.presentation.dto.request;

import java.util.List;

public record PostPublishedDto(
        String title,
        String content,
        Long seriesId,
        List<String> tags,
        String status
) {

}
