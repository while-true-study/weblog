package com.example.blog.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "reindexExecutor")
    public Executor reindexExecutor() { // 재색인용
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);      // 처음엔 1개로 시작
        executor.setMaxPoolSize(1);       // 최대로 1개
        executor.setQueueCapacity(10);    // 작업 몰리면 10개까지 큐
        executor.setThreadNamePrefix("reindex-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "postSyncExecutor")
    public Executor postSyncExecutor() { // 동기용
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("post-sync-");
        executor.initialize();
        return executor;
    }
}