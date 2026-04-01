package com.example.blog.series.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.series.presentation.dto.response.SeriesGetDto;
import com.example.blog.series.service.SeriesService;
import com.example.blog.user.entity.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Series", description = "시리즈 관련 API")
@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    @Operation(summary = "내 시리즈 목록 조회", description = "로그인한 사용자의 시리즈 목록을 조회합니다. (로그인 필요)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ApiResponse<List<SeriesGetDto>> getSeries(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<SeriesGetDto> seriesList = seriesService.getMySeries(principal.getId());
        return ApiResponse.success(seriesList);
    }
}
