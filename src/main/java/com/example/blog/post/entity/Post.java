package com.example.blog.post.entity;

import com.example.blog.categories.entity.Categories;
import com.example.blog.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private User authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private Categories categoriesId;

    private String title;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    private PostStatus postStatus;

    private Long viewCount;

    private Long likeCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
