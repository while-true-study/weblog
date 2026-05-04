package com.example.blog.search.mapper;

import com.example.blog.post.entity.Post;
import com.example.blog.search.outbox.dto.PostOutboxPayload;
import org.springframework.stereotype.Component;

@Component
public class PostSearchPayloadMapper {

    public PostOutboxPayload toPayload(Post post) {
        return PostOutboxPayload.builder()
                .postId(post.getPostId())
                .version(post.getSyncVersion())
                .build();
    }
}
