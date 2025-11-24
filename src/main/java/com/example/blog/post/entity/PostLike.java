package com.example.blog.post.entity;

import com.example.blog.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "post_like")
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postLikeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private Post postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private User userId;

    private LocalDateTime createdAt;
}
