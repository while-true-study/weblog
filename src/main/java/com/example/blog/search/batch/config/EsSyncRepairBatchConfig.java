package com.example.blog.search.batch.config;

import com.example.blog.search.batch.dto.RepairResult;
import com.example.blog.search.service.SearchIndexRepairService;
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

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class EsSyncRepairBatchConfig {

    private final SearchIndexRepairService searchIndexRepairService;

    @Bean
    public Job esSyncRepairJob(
            JobRepository jobRepository,
            Step repairPostSearchIndexStep
    ) {
        return new JobBuilder("esSyncRepairJob", jobRepository)
                .start(repairPostSearchIndexStep)
                .build();
    }

    @Bean
    public Step repairPostSearchIndexStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("repairPostSearchIndexStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters params = contribution.getStepExecution()
                            .getJobExecution()
                            .getJobParameters();

                    String fromParam = params.getString("from");
                    RepairResult result = (fromParam != null)
                            ? searchIndexRepairService.repair(LocalDateTime.parse(fromParam))
                            : searchIndexRepairService.repairAll();

                    contribution.getStepExecution().getExecutionContext()
                            .putInt("reindexed", result.getReindexed());
                    contribution.getStepExecution().getExecutionContext()
                            .putInt("deleted", result.getDeleted());
                    contribution.getStepExecution().getExecutionContext()
                            .putInt("skipped", result.getSkipped());

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
