package com.example.blog.search.presentation.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BatchTest", description = "배치 수동 실행 테스트용 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/test/batch")
public class BatchTestController {

    private final JobOperator jobOperator;
    private final Job esSyncRepairJob;

    @Operation(summary = "ES 동기화 복구 배치 수동 실행", description = "MySQL ↔ Elasticsearch 불일치를 복구하는 배치를 수동으로 실행합니다.")
    @PostMapping("/repair-es")
    public String runEsRepairBatch(
            @Parameter(description = "복구 시작 기준 시각 (ISO 8601, 예: 2024-01-01T00:00:00)")
            @RequestParam(required = false) String from
    ) throws Exception {
        long requestTime = System.nanoTime();

        JobParametersBuilder builder = new JobParametersBuilder()
                .addLong("requestTime", requestTime, true);

        if (from != null) {
            builder.addString("from", from, true);
        }

        jobOperator.start(esSyncRepairJob, builder.toJobParameters());
        return "ok";
    }
}