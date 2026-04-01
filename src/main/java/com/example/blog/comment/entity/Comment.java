package com.example.blog.comment.entity;

import com.example.blog.post.entity.Post;
import com.example.blog.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommentStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Builder
    private Comment(Post post, User author, String content) {
        this.post = post;
        this.author = author;
        this.content = content;
        this.status = CommentStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Comment create(Post post, User author, String content) {
        return Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build();
    }

    public void updateContent(String content) {
        validateActive();
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void softDelete() {
        validateActive();
        this.status = CommentStatus.DELETED;
        this.content = "삭제된 댓글입니다.";
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.status == CommentStatus.DELETED;
    }

    private void validateActive() {
        if (this.status == CommentStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 댓글입니다.");
        }
    }
}