package com.example.blog.postread.entity;

import com.example.blog.post.entity.Post;   // 프로젝트에 맞게 수정
import com.example.blog.user.entity.User;   // 프로젝트에 맞게 수정
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "post_read_history",
        indexes = {
                @Index(name = "idx_read_user_time", columnList = "user_id, last_read_at")
        }
)
public class PostReadHistory {

    @EmbeddedId
    private PostReadHistoryId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @Column(name = "read_count", nullable = false)
    private int readCount;

    private PostReadHistory(User user, Post post) {
        this.user = user;
        this.post = post;
        this.id = new PostReadHistoryId(user.getUserId(), post.getPostId());
        this.lastReadAt = LocalDateTime.now();
        this.readCount = 1;
    }

    public static PostReadHistory firstRead(User user, Post post) {
        return new PostReadHistory(user, post);
    }

    public void markRead() {
        this.lastReadAt = LocalDateTime.now();
        this.readCount += 1;
    }
}
