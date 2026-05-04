package com.example.blog.search.outbox.handler;

import com.example.blog.search.outbox.entity.OutboxEvent;

import java.time.Duration;

public interface OutboxEventHandler {

    boolean supports(OutboxEvent event);

    void handle(OutboxEvent event);

    default void onSuccess(OutboxEvent event, Duration duration) {
    }

    default void onFailure(OutboxEvent event, Exception exception) {
    }

    default void onRetry(OutboxEvent event) {
    }
}
