package com.example.blog.search.outbox.service;

import com.example.blog.post.entity.Post;
import com.example.blog.search.mapper.PostSearchPayloadMapper;
import com.example.blog.search.outbox.dto.PostOutboxPayload;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostOutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final PostSearchPayloadMapper payloadMapper;
    private final OutboxPayloadSerializer serializer;

    public void createCreatedEvent(Post post) {
        save(post, OutboxEventType.CREATED);
    }

    public void createUpdatedEvent(Post post) {
        save(post, OutboxEventType.UPDATED);
    }

    public void createDeletedEvent(Post post) {
        save(post, OutboxEventType.DELETED);
    }

    private void save(Post post, OutboxEventType eventType) {
        PostOutboxPayload payload = payloadMapper.toPayload(post);
        String payloadJson = serializer.serialize(payload);

        OutboxEvent event = new OutboxEvent(
                "POST",
                post.getPostId(),
                eventType,
                payloadJson,
                post.getSyncVersion()
        );

        outboxEventRepository.save(event);
    }
}