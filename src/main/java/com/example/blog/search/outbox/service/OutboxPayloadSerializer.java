package com.example.blog.search.outbox.service;

import com.example.blog.search.outbox.dto.PostOutboxPayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class OutboxPayloadSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(PostOutboxPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload 직렬화 실패", e);
        }
    }

    public PostOutboxPayload deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, PostOutboxPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox payload 역직렬화 실패", e);
        }
    }
}
