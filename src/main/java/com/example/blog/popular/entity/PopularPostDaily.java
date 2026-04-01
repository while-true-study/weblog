package com.example.blog.popular.entity;

import com.example.blog.post.entity.Post;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "popular_posts_daily",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_popular_posts_daily_date_post", columnNames = {"target_date", "post_id"}),
                @UniqueConstraint(name = "uk_popular_posts_daily_date_rank", columnNames = {"target_date", "rank_no"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PopularPostDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    @Column(nullable = false)
    private Double score;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
