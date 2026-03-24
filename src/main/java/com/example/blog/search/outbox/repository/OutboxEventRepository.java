package com.example.blog.search.outbox.repository;

import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusAndNextRetryAtLessThanEqualOrderByIdAsc(
            OutboxEventStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    List<OutboxEvent> findByStatusAndProcessedAtBefore(
            OutboxEventStatus status,
            LocalDateTime threshold
    );
}