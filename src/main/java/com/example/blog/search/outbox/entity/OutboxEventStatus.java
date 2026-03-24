package com.example.blog.search.outbox.entity;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}