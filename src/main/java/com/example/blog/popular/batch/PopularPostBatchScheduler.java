package com.example.blog.popular.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class PopularPostBatchScheduler {

    private final JobOperator jobOperator;
    private final Job popularPostAggregationJob;

    @Scheduled(cron = "0 35 1 * * *")
    public void runDailyAggregation() throws Exception {
        LocalDate targetDate = LocalDate.now().minusDays(1);

        jobOperator.start(
                popularPostAggregationJob,
                new JobParametersBuilder()
                        .addString("targetDate", targetDate.toString())
                        .addLong("run.id", System.currentTimeMillis())
                        .toJobParameters()
        );
    }
}