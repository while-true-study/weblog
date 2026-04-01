package com.example.blog.search.monitoring;

import io.micrometer.core.instrument.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class SearchSyncMetrics {

    private final MeterRegistry meterRegistry;

    private final AtomicInteger pendingCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);

    @PostConstruct
    void init() {
        Gauge.builder("blog.search.outbox.pending.count", pendingCount, AtomicInteger::get)
                .description("Current pending outbox event count")
                .register(meterRegistry);

        Gauge.builder("blog.search.outbox.failed.count", failedCount, AtomicInteger::get)
                .description("Current failed outbox event count")
                .register(meterRegistry);
    }

    public void setPendingCount(int value) {
        pendingCount.set(value);
    }

    public void setFailedCount(int value) {
        failedCount.set(value);
    }

    public void incrementSyncSuccess(String eventType) {
        meterRegistry.counter(
                "blog.search.es.sync.total",
                "eventType", eventType,
                "result", "success"
        ).increment();
    }

    public void incrementSyncFailure(String eventType) {
        meterRegistry.counter(
                "blog.search.es.sync.total",
                "eventType", eventType,
                "result", "failure"
        ).increment();
    }

    public void incrementRetry(String eventType) {
        meterRegistry.counter(
                "blog.search.outbox.retry.total",
                "eventType", eventType
        ).increment();
    }

    public void recordProcessingLatency(String eventType, Duration duration) {
        meterRegistry.timer(
                "blog.search.outbox.processing.latency",
                "eventType", eventType
        ).record(duration);
    }
}