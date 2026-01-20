package com.example.blog.series.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.series.presentation.dto.response.SeriesGetDto;
import com.example.blog.series.service.SeriesService;
import com.example.blog.user.entity.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    @GetMapping
    public ApiResponse<List<SeriesGetDto>> getSeries(
            @AuthenticationPrincipal CustomUserPrincipal principal
            ) {
        List<SeriesGetDto> seriesList = seriesService.getMySeries(principal.getId());
        return ApiResponse.success(seriesList);
    }
}
