package com.example.blog.search.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class OutboxPayloadSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload 직렬화 실패", e);
        }
    }

    public <T> T deserialize(String payload, Class<T> payloadType) {
        try {
            return objectMapper.readValue(payload, payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload 역직렬화 실패", e);
        }
    }
}
