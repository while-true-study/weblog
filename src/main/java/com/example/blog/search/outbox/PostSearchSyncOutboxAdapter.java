package com.example.blog.search.outbox;

import com.example.blog.post.entity.Post;
import com.example.blog.search.application.port.PostSearchSyncPort;
import com.example.blog.search.outbox.entity.OutboxEvent;
import com.example.blog.search.outbox.entity.OutboxEventType;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostSearchSyncOutboxAdapter implements PostSearchSyncPort {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void enqueuePostChanged(Post post) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("postId", post.getPostId());
            payloadMap.put("version", post.getSyncVersion());

            String payload = objectMapper.writeValueAsString(payloadMap);

            OutboxEvent event = new OutboxEvent(
                    "POST",
                    post.getPostId(),
                    OutboxEventType.UPDATED, // 네 enum 값에 맞게 바꿔라
                    payload,
                    post.getSyncVersion()
            );

            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Outbox payload 생성 실패", e);
        }
    }
}