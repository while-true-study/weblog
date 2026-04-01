package com.example.blog.popular.batch;

import com.example.blog.popular.service.PopularAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class PopularPostBatchConfig {

    private final PopularAggregationService popularAggregationService;

    @Bean
    public Job popularPostAggregationJob(
            JobRepository jobRepository,
            Step aggregateDailyPopularPostsStep
    ) {
        return new JobBuilder("popularPostAggregationJob", jobRepository)
                .start(aggregateDailyPopularPostsStep)
                .build();
    }

    @Bean
    public Step aggregateDailyPopularPostsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("aggregateDailyPopularPostsStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters jobParameters = contribution.getStepExecution()
                            .getJobExecution()
                            .getJobParameters();

                    String targetDateParam = jobParameters.getString("targetDate");
                    LocalDate targetDate = (targetDateParam != null)
                            ? LocalDate.parse(targetDateParam)
                            : LocalDate.now().minusDays(1);

                    popularAggregationService.aggregateDaily(targetDate, 100);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}