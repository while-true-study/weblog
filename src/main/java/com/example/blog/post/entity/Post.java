package com.example.blog.post.entity;

import com.example.blog.categories.entity.Categories;
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
    @JoinColumn(name = "category_id", nullable = false)
    private Categories category;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostTag> postTags = new ArrayList<>();

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
}