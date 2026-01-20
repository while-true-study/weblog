package com.example.blog.series.presentation.dto.response;

import com.example.blog.series.entity.Series;

public record SeriesGetDto(
        Long id,
        String name
) {
    public static SeriesGetDto from(Series s) {
        return new SeriesGetDto(s.getId(), s.getName());
    }
}
