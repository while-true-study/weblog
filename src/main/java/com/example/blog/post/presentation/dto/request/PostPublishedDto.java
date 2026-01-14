package com.example.blog.post.presentation.dto.request;

import java.util.List;

public record PostPublishedDto(
        String category,
        String content,
        String status,
        List<String> tags,
        String title
) {

}
