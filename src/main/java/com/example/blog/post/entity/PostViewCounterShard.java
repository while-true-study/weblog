package com.example.blog.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "post_view_counter_shard")
@Getter
@Setter
@NoArgsConstructor
public class PostViewCounterShard {

    @EmbeddedId
    private PostViewCounterShardId id;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;
}