package com.example.blog.series.service;

import com.example.blog.series.presentation.dto.response.SeriesGetDto;

import java.util.List;

public interface SeriesService {
    List<SeriesGetDto> getMySeries(Long userId);
}
