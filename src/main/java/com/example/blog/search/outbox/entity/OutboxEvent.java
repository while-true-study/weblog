package com.example.blog.search.outbox.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String aggregateType; // ex) POST

    @Column(nullable = false)
    private Long aggregateId; // postId

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventType eventType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(nullable = false)
    private int retryCount;

    private LocalDateTime nextRetryAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private String lastErrorMessage;

    public OutboxEvent(
            String aggregateType,
            Long aggregateId,
            OutboxEventType eventType,
            String payload,
            Long version
    ) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.version = version;
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
        this.nextRetryAt = LocalDateTime.now();
    }

    public void markProcessing() {
        this.status = OutboxEventStatus.PROCESSING;
    }

    public void markSuccess() {
        this.status = OutboxEventStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
        this.lastErrorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxEventStatus.FAILED;
        this.retryCount += 1;
        this.lastErrorMessage = errorMessage;
        this.nextRetryAt = LocalDateTime.now().plusSeconds(calculateBackoffSeconds());
    }

    public void requeueIfRetryable() {
        this.status = OutboxEventStatus.PENDING;
    }

    private long calculateBackoffSeconds() {
        return switch (this.retryCount) {
            case 1 -> 10;
            case 2 -> 30;
            case 3 -> 60;
            default -> 300;
        };
    }

    public boolean canRetry() {
        return this.retryCount < 5;
    }
}