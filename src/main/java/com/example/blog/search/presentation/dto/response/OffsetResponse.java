package com.example.blog.search.presentation.dto.response;

import java.util.List;

public record OffsetResponse<T>(
        List<T> items,
        int offset,
        int limit,
        boolean hasNext,
        Integer nextOffset
) {
    public static <T> OffsetResponse<T> of(List<T> items, int offset, int limit, boolean hasNext) {
        return new OffsetResponse<>(
                items,
                offset,
                limit,
                hasNext,
                hasNext ? offset + items.size() : null
        );
    }
}
