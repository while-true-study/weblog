package com.example.blog.post.entity;

import com.example.blog.series.entity.Series;
import com.example.blog.tag.entity.PostTag;
import com.example.blog.tag.entity.Tag;
import com.example.blog.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id") // nullable 허용(시리즈 미지정 가능)
    private Series series;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();

    @Column(nullable = false)
    private Long syncVersion = 1L;

    public void increaseSyncVersion() {
        if (this.syncVersion == null) {
            this.syncVersion = 1L;
        } else {
            this.syncVersion += 1;
        }
    }

    public void softDelete() {
        LocalDateTime now = LocalDateTime.now();
        this.postStatus = PostStatus.DELETED;
        this.deletedAt = now;
        this.updatedAt = now;
        increaseSyncVersion();
    }

    public void addTag(Tag tag) {
        this.postTags.add(new PostTag(this, tag));
    }

    public void clearTags() {
        this.postTags.clear();
    }

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

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.viewCount == null) this.viewCount = 0L;
        if (this.likeCount == null) this.likeCount = 0L;
        if (this.syncVersion == null) this.syncVersion = 1L;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}