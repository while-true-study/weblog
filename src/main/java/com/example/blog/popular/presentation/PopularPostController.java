package com.example.blog.popular.presentation;

import com.example.blog.popular.presentation.dto.response.PopularPostResponse;
import com.example.blog.popular.service.PopularArchiveQueryService;
import com.example.blog.popular.service.PopularQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "PopularPost", description = "인기 게시글 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/popular-posts")
public class PopularPostController {

    private final PopularQueryService popularQueryService;
    private final PopularArchiveQueryService popularArchiveQueryService;
    private final JobOperator jobOperator;
    private final Job popularPostAggregationJob;

    @Operation(summary = "일별 인기 게시글 조회 (실시간)", description = "Redis 기반 실시간 인기 게시글을 조회합니다. date 미입력 시 오늘 기준.")
    @GetMapping("/daily")
    public List<PopularPostResponse> getDailyPopularPosts(
            @Parameter(description = "조회 날짜 (yyyy-MM-dd, 기본값: 오늘)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @Parameter(description = "조회 개수 (기본값 10)") @RequestParam(defaultValue = "10") int limit
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return popularQueryService.getDailyPopularPosts(targetDate, limit);
    }

    @Operation(summary = "일별 인기 게시글 조회 (아카이브)", description = "MySQL에 저장된 배치 집계 결과를 조회합니다.")
    @GetMapping("/daily/archive")
    public List<PopularPostResponse> getArchivedDailyPopularPosts(
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)", required = true)
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return popularArchiveQueryService.getArchivedDailyPopularPosts(date);
    }

    @Operation(summary = "인기 게시글 배치 수동 실행 (테스트용)", description = "특정 날짜의 인기 게시글 집계 배치를 수동으로 실행합니다.")
    @PostMapping("/popular")
    public String runPopularBatch(
            @Parameter(description = "집계 대상 날짜 (yyyy-MM-dd)") @RequestParam String targetDate
    ) throws Exception {
        jobOperator.start(
                popularPostAggregationJob,
                new JobParametersBuilder()
                        .addString("targetDate", targetDate, true)
                        .addLong("requestTime", System.currentTimeMillis(), false)
                        .toJobParameters()
        );
        return "ok";
    }
}